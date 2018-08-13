package peer;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

import utils.Utils;

public class RegisterServlet extends DefaultServlet {
	private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());


	private final PeerList peerList;
	
	public RegisterServlet(PeerList peerList) {
		this.peerList = peerList;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Peer p = new Peer(req.getRemoteAddr() + ":" + req.getRemotePort());
		try {
			p.checkVersion();
			boolean success = peerList.addPeer(p);
			Utils.responseFromString(""+success, resp);
			LOGGER.info("New peer registered");
		}catch (Exception e) {
			LOGGER.warning("Registering peer failed: did not answer with a correct version");
			Utils.responseFromString("Invalid response: couldn't probe the version on your machine", resp);
		}
		
	}
}
