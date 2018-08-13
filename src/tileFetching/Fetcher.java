package tileFetching;

import java.io.IOException;
import java.io.OutputStream;

public interface Fetcher {
	public long fetch(TileID id, OutputStream writeTo) throws IOException;
}
