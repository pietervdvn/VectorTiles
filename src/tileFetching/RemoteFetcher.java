package tileFetching;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import peer.Peer;

/**
 * Fetches the images live from some other source, e.g. OSM
 * 
 * @author pietervdvn
 *
 */
public class RemoteFetcher implements Fetcher {
	private static final Logger LOGGER = Logger.getLogger(RemoteFetcher.class.getName());


	private final String urlTemplate;


	public RemoteFetcher(Peer peer, String layer) {
		this(peer.getURL() + "/cachedtiles-"+layer+"?z={z}&x={x}&y={y}");
	}

	public RemoteFetcher(String url) {
		this.urlTemplate = url;
	}

	public long fetch(TileID id, OutputStream writeTo) throws IOException {
		URL url = new URL(id.fillURL(urlTemplate));
		LOGGER.info("Getting tile "+id.fillURL(urlTemplate));
		URLConnection conn = url.openConnection();
		long totalCopied = 0;
		try (InputStream in = conn.getInputStream()) {
			byte[] buffer = new byte[8 * 1024];
			int len;
			while ((len = in.read(buffer)) > 0) {
				totalCopied += len;
				writeTo.write(buffer, 0, len);
			}
		}
		return totalCopied;
	}

}
