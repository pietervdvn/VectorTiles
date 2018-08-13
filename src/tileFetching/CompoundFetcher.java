package tileFetching;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class CompoundFetcher implements Fetcher{
	
	private final List<Fetcher> fetchers;
	
	public CompoundFetcher(Fetcher...fetchers) {
		this.fetchers = Arrays.asList(fetchers);
	}

	@Override
	public long fetch(TileID id, OutputStream writeTo) throws IOException {
		for (Fetcher fetcher : fetchers) {
			try {
				long fetched = fetcher.fetch(id, writeTo);
				if(fetched != 0) {
					return fetched;
				}
			}catch (IOException e) {
				// Failure case -> try the next fetcher
			}
		}
		return 0;
	}

}
