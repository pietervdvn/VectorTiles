package main;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import peer.Peer;
import peer.PeerList;
import peer.PeerListFetcher;
import peer.PeerListServlet;
import peer.RegisterServlet;
import swingRender.SimpleWindow;
import swingRender.VectorTilePanel;
import tileFetching.CachedFetcher;
import tileFetching.CachingFetcher;
import tileFetching.CompoundFetcher;
import tileFetching.Fetcher;
import tileFetching.RemoteFetcher;
import tileFetching.TileServlet;
import vectortile.TagDecoder;
import vectortile.data.VectorTile;
import vectortile.optimizers.TagCompactor;
import vectortile.optimizers.VectorTileClipper;
import vectortile.optimizers.VectorTileReshuffler;
import vectortile.serialization.BinaryDecoder;
import vectortile.serialization.BinaryEncoder;
import vectortile.serialization.OSMXMLParser;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	public final static int VERSION = 1;

	public static void main(String[] args) throws IOException, XMLStreamException, InterruptedException {

		TagDecoder td = new TagDecoder();

		OSMXMLParser p = new OSMXMLParser();
		VectorTile vt = p.deserialize("/home/pietervdvn/Downloads/RawOsm.xml");

		vt = new VectorTileClipper(vt).createOptimized();
		vt = new TagCompactor(vt).createOptimized();
		vt = new VectorTileReshuffler(vt).createOptimized();

		//vt = testSer(vt);
		render(vt);
	}
	
	public static VectorTile testSer(VectorTile vt) throws IOException, XMLStreamException {
		BinaryEncoder ser = new BinaryEncoder(vt);
		String path = "/home/pietervdvn/Downloads/VectorTile.bin";
		ser.serialize(path);
		
		return new BinaryDecoder().deserialize(path);
	}
	
	public static void render(VectorTile vt) throws InterruptedException, IOException {
		SimpleWindow sw = new SimpleWindow();
		sw.add(new VectorTilePanel(vt));
		Thread.sleep(50000);
		sw.dispose();
		System.exit(0);
	}

	public static void main0(String[] args) throws Exception {

		LOGGER.info("Loading MapHOST v" + VERSION);

		AppContext appContext = new AppContext("prod.properties");
		AppContext.configLogger("log.properties");

		String cacheDir = appContext.getProperty("cache-dir");
		LOGGER.info("Caching dir is " + cacheDir);

		Server server = new Server(appContext.getPropertyInt("port"));
		WebAppContext context = new WebAppContext();
		PeerList pl = initPeerHandling(appContext);

		List<String> knownLayers = Arrays.asList(appContext.getProperty("layers").split(";"));

		context.setResourceBase("www");

		for (String layer : knownLayers) {
			LOGGER.info("Initializing layer " + layer);
			RemoteFetcher source = new RemoteFetcher(appContext.getProperty("layer-" + layer + "-source"));
			PeerListFetcher peerFetcher = new PeerListFetcher(pl, layer);

			Fetcher compoundFetcher = appContext.getFlag("peers-only") ? peerFetcher
					: new CompoundFetcher(peerFetcher, source);

			CachedFetcher cacher = new CachedFetcher(cacheDir);
			Fetcher cached = new CachingFetcher(compoundFetcher, cacher);

			context.addServlet(new ServletHolder(new TileServlet(cached, layer)), "/tiles-" + layer);
			context.addServlet(new ServletHolder(new TileServlet(cacher, layer)), "/cachedtiles-" + layer);

		}

		context.addServlet(new ServletHolder(new FixedStringServlet("" + VERSION)), "/version");
		context.addServlet(new ServletHolder(new RegisterServlet(pl)), "/register");
		context.addServlet(new ServletHolder(new PeerListServlet(pl)), "/peerlist");
		context.addServlet(new ServletHolder(new FixedStringServlet(appContext.getProperty("layers"))), "/layers");
		server.setHandler(context);
		server.start();
		LOGGER.info("The server is listening...");

	}

	private static PeerList initPeerHandling(AppContext appContext) {
		Peer rootServer = new Peer(appContext.getProperty("root-server"));
		PeerList pl = new PeerList();
		pl.addPeer(rootServer);
		return pl;
	}

}
