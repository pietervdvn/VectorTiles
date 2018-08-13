package tileFetching;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Fetcher which only can use the cache; returns null if not in the cache
 */
public class CachedFetcher implements Fetcher {

	private final String chachingDir;

	public CachedFetcher(String cachingDir) {
		this.chachingDir = cachingDir;
	}

	@Override
	public long fetch(TileID id, OutputStream writeTo) throws IOException {
		if(isInCache(id)) {
			return Files.copy(id.getPath(chachingDir), writeTo);
		}else {
			return 0;
		}
	}

	public OutputStream storageFor(TileID id) throws IOException {
		Path p = id.getPath(chachingDir);
		Files.createDirectories(p.getParent());
		return Files.newOutputStream(p);
	}

	public boolean isInCache(TileID id) {
		return Files.exists(id.getPath(chachingDir));
	}

	public void purgeCache(TileID id) {
		try {
			Files.delete(id.getPath(chachingDir));
		} catch (IOException e) {
		}
	}

}
