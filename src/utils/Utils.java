package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletResponse;

public class Utils {

	/**
	 * Sends response to the calling process.
	 */
	public static void responseFromString(String data, HttpServletResponse resp) throws IOException {
		try (BufferedWriter sos = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()))) {
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			sos.write(data);
			sos.flush();
		}
	}

	public static String loadFromURL(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		String result = "";
		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			String read = in.readLine();
			while (read != null) {
				result += read + "\n";
				read = in.readLine();
			}
		}
		return result.substring(0, result.length() - 1);
	}

}
