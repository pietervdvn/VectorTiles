package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.xml.stream.XMLStreamException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import peer.Peer;
import peer.PeerList;
import peer.PeerListFetcher;
import peer.PeerListServlet;
import peer.RegisterServlet;
import tileFetching.CachedFetcher;
import tileFetching.CachingFetcher;
import tileFetching.CompoundFetcher;
import tileFetching.Fetcher;
import tileFetching.RemoteFetcher;
import tileFetching.TileServlet;
import utils.Utils;
import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Tags;
import vectortile.data.VectorTile;
import vectortile.optimizers.TagCompactor;
import vectortile.optimizers.VectorTileClipper;
import vectortile.optimizers.VectorTileReshuffler;
import vectortile.serialization.BinaryDecoder;
import vectortile.serialization.BinaryEncoder;
import vectortile.serialization.OSMXMLParser;
import vectortile.style.EncodedCondition;
import vectortile.style.StyleSheet;
import vectortile.style.StyleSheetJsonParser;
import vectortile.swingRender.SimpleWindow;
import vectortile.swingRender.VectorTilePanel;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	public final static int VERSION = 1;

	private final static String path = "/home/pietervdvn/Downloads/VectorTile.bin";

	public static void main(String[] args) throws Exception {
		VectorTile vt = fromSer();
		// System.out.println(vt.getDecoder().getWaytags());
		render(vt);

		/*
		 * Tags testTags = new Tags("leisure","garden","name","Blokstraat"); testTags =
		 * td.buildTagsFor(Types.WAY, testTags, true); testTags =
		 * vt.getDecoder().buildTagsFor(Types.WAY, testTags, false);
		 * System.out.println(testTags);
		 * 
		 * 
		 * System.out.println(ec.resolve(testTags));
		 */
	}

	public static VectorTile fromOSM() throws FileNotFoundException, IOException, XMLStreamException {
		VectorTile vt = new OSMXMLParser().download(//
				OSMXMLParser.OPENSTREETMAP_DATA, //
				51.215, 3.219, 51.2165, 3.222);

		vt = optimize(vt);
		testSer(vt);
		return vt;
	}

	public static VectorTile fromSer() throws FileNotFoundException, IOException, XMLStreamException {
		return new BinaryDecoder().deserialize(path);
	}

	public static VectorTile fromFile() throws FileNotFoundException, IOException, XMLStreamException {

		OSMXMLParser p = new OSMXMLParser();
		VectorTile vt = p.deserialize("/home/pietervdvn/Downloads/RawOsm.xml");
		vt = optimize(vt);
		vt = testSer(vt);

		return vt;
	}

	public static VectorTile optimize(VectorTile vt) {
		vt = new VectorTileClipper(vt).createOptimized();
		vt = new TagCompactor(vt).createOptimized();
		vt = new VectorTileReshuffler(vt).createOptimized();
		return vt;
	}

	public static VectorTile testSer(VectorTile vt) throws IOException, XMLStreamException {
		BinaryEncoder ser = new BinaryEncoder(vt);
		ser.serialize(path);

		return new BinaryDecoder().deserialize(path);
	}

	public static void render(VectorTile vt) throws InterruptedException, IOException {
		TagDecoder td = new TagDecoder();

		StyleSheetJsonParser ssjp = new StyleSheetJsonParser();
		StyleSheet sheet = ssjp.parseStyleSheet(Utils.readFile("res/styles/Landuse.style.json"));

		JPanel vectorPanel = new VectorTilePanel(vt, td, sheet);
		SimpleWindow sw = new SimpleWindow(vectorPanel);
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
