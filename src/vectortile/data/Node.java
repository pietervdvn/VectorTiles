package vectortile.data;

import vectortile.TagDecoder;
import vectortile.Types;

/**
 * Represents a point. Actual latitude and longitude are scaled wrt a reference
 * point
 *
 */
public class Node extends Taggable {

	/**
	 * Relative latitudes and longitudes to the vector tile UL corner, *10'000'000
	 */
	public final int lat, lon;
	public final static int NODE_SCALING_FACTOR = 10000000;

	public Node(double latWGS84, double lonWGS84, double refLatWGS84, double refLonWGS84) {
		this((int) ((latWGS84 - refLatWGS84) * NODE_SCALING_FACTOR), //
				(int) ((latWGS84 - refLatWGS84) * NODE_SCALING_FACTOR));
	}

	public Node(int lat, int lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public String toString(TagDecoder td) {
		return "node: " + lat + ", " + lon + super.toString(Types.NODE, td);
	}

	@Override
	public String toString() {
		return "node: " + lat + ", " + lon;
	}

	public int getLat() {
		return lat;
	}

	public int getLon() {
		return lon;
	}

	public double getWGS84Lat(double refLatWGS84) {
		return refLatWGS84 + ((double) lat) / NODE_SCALING_FACTOR;
	}

	public double getWGS84Lon(double refLonWGS84) {
		return refLonWGS84 + ((double) lon) / NODE_SCALING_FACTOR;
	}

}
