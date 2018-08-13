package tileFetching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public class CachingFetcher implements Fetcher {
	private static final Logger LOGGER = Logger.getLogger(CachedFetcher.class.getName());

	private final Fetcher remote;
	private final CachedFetcher cacher;

	public CachingFetcher(Fetcher remote, CachedFetcher cacher) {
		this.remote = remote;
		this.cacher = cacher;
	}

	@Override
	public long fetch(TileID id, OutputStream writeTo) throws IOException {

		try {
			long read = cacher.fetch(id, writeTo);
			if (read != 0) {
				return read;
			}else {
				// Empty cached tile -> remove it!
				cacher.purgeCache(id);
			}
		} catch (FileNotFoundException e) {
		}
		
		// fallback: was not in the cache or cached file was empty
		
		try (OutputStream storeTo = cacher.storageFor(id)) {
			long copied = remote.fetch(id, storeTo);
			if (copied == 0) {
				cacher.purgeCache(id);
				return 0;
			}

			LOGGER.info("Created cached image for " + id);
			return cacher.fetch(id, writeTo);
		}
	}

}
