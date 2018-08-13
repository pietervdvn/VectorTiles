package osm2vectortile;

import java.util.Map;

import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.VectorTile;
import vectortile.data.Way;

public class RawData {
	private final Map<Long, Node> nodes;
	private final Map<Long, Way> ways;
	private final Map<Long, Relation> relations;

	// Lat & lon (times 1000) of the upper left corner of the reference point
	private final int lat, lon;

	public RawData(int lat, int lon, Map<Long, Node> nodes, Map<Long, Way> ways, Map<Long, Relation> relations) {
		this.lat = lat;
		this.lon = lon;
		this.nodes = nodes;
		this.ways = ways;
		this.relations = relations;
	}

	/**
	 * Creates a vector tile within the given WGS84 coordinates. Note that these are
	 * actual, unmodified coordinates!
	 * 
	 * @param minLat
	 * @param minLon
	 * @param maxLat
	 * @param maxLon
	 * @return 
	 */
	public VectorTile createTile(double minLat, double minLon, double maxLat, double maxLon, boolean clip) {
		if (minLon > maxLon || minLat > maxLat) {
			throw new IllegalArgumentException("Minimal lat/lon is bigger then maximal lat/lon");
		}

		// Convert 'classic coors to the referenced coordinates
		BBox bbox = new BBox(this, convertLatForBBOX(minLat), convertLonForBBOX(minLon),convertLatForBBOX(maxLat),convertLonForBBOX(maxLon));
		return bbox.asVectorTile(clip);
	}

	private int convertLatForBBOX(double origLat) {
		return (int) (((origLat * VectorTile.COORDINATES_SCALING_FACTOR) - lat) * (Node.NODE_SCALING_FACTOR/VectorTile.COORDINATES_SCALING_FACTOR));
	}

	private int convertLonForBBOX(double origLon) {
		return (int) (((origLon * VectorTile.COORDINATES_SCALING_FACTOR) - lon) * (Node.NODE_SCALING_FACTOR/VectorTile.COORDINATES_SCALING_FACTOR));
	}

	public Map<Long, Node> getNodes() {
		return nodes;
	}

	public Map<Long, Way> getWays() {
		return ways;
	}

	public Map<Long, Relation> getRelations() {
		return relations;
	}

	public int getLon() {
		return lon;
	}

	public int getLat() {
		return lat;
	}

}
