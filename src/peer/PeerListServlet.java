package peer;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

import utils.Utils;

public class PeerListServlet extends DefaultServlet{
	
	private final PeerList peerList;
	
	public PeerListServlet(PeerList peerList) {
		this.peerList = peerList;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String data = peerList.toSpaceSep();
		Utils.responseFromString(data, resp);
	}
	
}
