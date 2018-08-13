package tileFetching;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

public class TileServlet extends DefaultServlet {
	private static final Logger LOGGER = Logger.getLogger(TileServlet.class.getName());

	private final Fetcher fetcher;
	private final String layer;
	
	public TileServlet(Fetcher fetcher, String layer) {
		this.fetcher = fetcher;
		this.layer = layer;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		TileID id = getTileID(req);
		try (OutputStream out = resp.getOutputStream()) {
			resp.setContentType("image/png");
			resp.setCharacterEncoding("UTF-8");
			fetcher.fetch(id, out);
		} catch (Exception exc) {
			LOGGER.log(Level.WARNING, "Unexpected behavior", exc);
		}		
		

	}
	
	private TileID getTileID(HttpServletRequest req) {
		int z = getParameterInt(req, "z");
		int x = getParameterInt(req, "x");
		int y = getParameterInt(req, "y");
		return new TileID(z, x, y, layer);
	}

	private static int getParameterInt(HttpServletRequest req, String name) {
		String parameter = req.getParameter(name);
		if(parameter == null || parameter.equals("")) {
			LOGGER.warning("Missing argument "+name);
		}
		return Integer.parseInt(parameter);
	}

}
