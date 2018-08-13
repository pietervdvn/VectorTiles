package peer;

import java.util.HashSet;
import java.util.Set;

public class PeerList {

	private final Set<Peer> peers = new HashSet<>();

	public boolean addPeer(Peer p) {
		return peers.add(p);
	}
	
	public Set<Peer> getPeers(){
		return peers;
	}

	public String toSpaceSep() {
		String result = "";
		for (Peer peer : peers) {
			result += peer.getURL() + " ";
		}
		return result.substring(0, result.length() - 1);
	}

}
