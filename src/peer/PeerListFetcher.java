package peer;

import java.io.IOException;
import java.io.OutputStream;

import tileFetching.Fetcher;
import tileFetching.RemoteFetcher;
import tileFetching.TileID;

public class PeerListFetcher implements Fetcher {

	private final PeerList peerList;
	private final String layer;

	public PeerListFetcher(PeerList peerList, String layer) {
		this.peerList = peerList;
		this.layer = layer;
	}
	

	@Override
	public long fetch(TileID id, OutputStream writeTo) throws IOException {
		for (Peer p : peerList.getPeers()) {
			RemoteFetcher r = new RemoteFetcher(p, layer);
			try {
				long copied =  r.fetch(id, writeTo);
				if(copied != 0) {
					return copied;
				}
			} catch (IOException e) {
			}
		}
		return 0;
	}

}
