package peer;

import java.io.IOException;
import java.util.Date;

import utils.Utils;

public class Peer {

	private final String url;
	private int version;
	private Date lastContacted;

	public Peer(String url) {
		this.url = "http://"+url;
	}

	public void checkVersion() throws IOException {
		String readVersion = Utils.loadFromURL(url + "/version");
		this.version = Integer.parseInt(readVersion);
		this.lastContacted = new Date();
	}

	@Override
	public String toString() {
		return "Peer " + url + " v" + (version == 0 ? "?" : version);
	}

	public String getURL() {
		return url;
	}
	
	public int getVersion() {
		return version;
	}
	
	public Date getLastContacted() {
		return lastContacted;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Peer)) {
			return false;
		}
		Peer p = (Peer) obj;
		return p.url.equals(this.url);
	}
	
	@Override
	public int hashCode() {
		return url.hashCode();
	}
	
	
}
