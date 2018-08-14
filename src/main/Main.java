package main;

import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JPanel;

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
import vectortile.TagDecoder;
import vectortile.datadownloader.CacheManager;
import vectortile.datadownloader.VectorTileID;
import vectortile.style.MasterSheet;
import vectortile.swingRender.LoadingPanel;
import vectortile.swingRender.SimpleWindow;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	public final static int VERSION = 1;

	// TODO encode osm-ids efficiently
	static CacheManager cm;
	static TagDecoder global;
	static MasterSheet sheet;
	static ExecutorService threads = Executors.newFixedThreadPool(1);

	public static void main(String[] args) throws Exception {

		AppContext appContext = new AppContext("prod.properties");
		// AppContext.configLogger("log.properties");

		cm = new CacheManager(appContext.getProperty("cache-dir"));
		global = new TagDecoder();
		sheet = new MasterSheet("res/styles");

		GridLayout gl = new GridLayout(3, 3);
		JPanel all = new JPanel(gl);
		double lat = 51.215;
		for (int r = 0; r < 2; r++) {
			double lon = 3.22;
			for (int l = 0; l < 2; l++) {
				all.add(panelFor(lat, lon));
				lon += 0.005;
			}
			lat -= 0.005;
		}
		threads.shutdown();
		new SimpleWindow(all);
	}

	private static JPanel panelFor(double lat, double lon) {
		final VectorTileID vid = new VectorTileID(lat, lon);
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					cm.retrieveOrDownload(vid, 0.005);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		threads.submit(task);
		return new LoadingPanel(vid, cm, global, sheet);
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
