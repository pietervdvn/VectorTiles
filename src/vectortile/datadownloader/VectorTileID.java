package vectortile.datadownloader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class VectorTileID {

	private final double lat, lon;

	public VectorTileID(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public String fillURL(String urlTemplate) {
		if (urlTemplate == null) {
			throw new NullPointerException("The URL-template is null!");
		}
		return urlTemplate.replace("{lat}", "" + lat).replace("{lon}", "" + lon);
	}

	public Path getPath(String template) {
		return Paths.get(this.fillURL(template));
	}

	public double getLat() {
		return lat;
	}
	
	public double getLon() {
		return lon;
	}
	
	@Override
	public String toString() {
		return "VectorTileID: " + lat + "," + lon;
	}

}
