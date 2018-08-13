package main;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

import utils.Utils;

public class FixedStringServlet extends DefaultServlet{

	private final String response;
	
	public FixedStringServlet(String response) {
		this.response = response;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Utils.responseFromString(response, resp);
	}
	
}
