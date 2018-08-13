package tileFetching;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TileID {

	private final int z, x, y;
	private final String layer;

	public TileID(int z, int x, int y, String layer) {
		this.z = z;
		this.x = x;
		this.y = y;
		this.layer = layer;
	}

	public String fillURL(String urlTemplate) {
		if(urlTemplate == null) {
			throw new NullPointerException("The URL-template is null!");
		}
		return urlTemplate.replace("{z}", "" + z).replace("{x}", "" + x).replace("{y}", "" + y).replace("{layer}", layer);
	}
	
	public Path getPath(String template) {
		return Paths.get(this.fillURL(template));
	}
	
	@Override
	public String toString() {
		return "TileID: "+layer+"/"+z+"/"+x+"/"+y;
	}

}
