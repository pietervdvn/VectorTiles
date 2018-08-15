package main;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JFrame;
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

	public static double INCREASE = 0.001;
	
	public static void main(String[] args) throws Exception {

		AppContext appContext = new AppContext("prod.properties");
		// AppContext.configLogger("log.properties");

		cm = new CacheManager(appContext.getProperty("cache-dir"));
		global = new TagDecoder();
		sheet = new MasterSheet("res/styles");

		int w = 4;
		int h = 3;
		GridLayout gl = new GridLayout(h, w);
		SimpleWindow f = new SimpleWindow();
		gl.setVgap(0);
		gl.setHgap(0);
		JPanel all = new JPanel(gl);
		all.setPreferredSize(new Dimension(w*333, h*333));
		double lat = 51.216;
		for (int r = 0; r < h; r++) {
			double lon = 3.218;
			for (int c = 0; c < w; c++) {
				all.add(panelFor(f, lat, lon));
				lon += INCREASE;
			}
			lat -= INCREASE;
		}
		threads.shutdown();
		f.add(all);
		f.goLive();
	}

	private static JPanel panelFor(JFrame f, double lat, double lon) {
		final VectorTileID vid = new VectorTileID(lat, lon);
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					cm.retrieveOrDownload(vid, INCREASE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		threads.submit(task);
		return new LoadingPanel(f, vid, cm, global, sheet);
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
