package vectortile.data;

import java.util.ArrayList;
import java.util.List;

import vectortile.TagDecoder;

/**
 * A vectortile contains all the information on a certain position.
 * 
 * @author pietervdvn
 *
 */
public class VectorTile {

	/**
	 * Latitude and longitude of the coordinate. This is in WGS84 format, where
	 * latitude and longitude have been multiplied by 1000 We assume that latitude
	 * and longitutde will never need to be more precise then 3 decimal points to
	 * indicate the upper left corner or a tile
	 */
	private final int minLat, minLon, maxLat, maxLon;
	public final static int COORDINATES_SCALING_FACTOR = Node.NODE_SCALING_FACTOR;

	/**
	 * Recodes the tags which appear a lot locally
	 */
	private final TagDecoder decoder;

	private final List<Node> nodes;
	private final List<Node> ghostNodes;
	private final List<Way> ways;
	private final int nodeEncodedWays;
	private final List<Relation> relations;

	public VectorTile(double minLatWGS84, double minLonWGS84, double maxLatWGS84, double maxLonWGS84, //
			List<Node> nodes, List<Way> ways, List<Relation> relations) {
		this(//
				(int) (minLatWGS84 * COORDINATES_SCALING_FACTOR), //
				(int) (minLonWGS84 * COORDINATES_SCALING_FACTOR), //
				(int) (maxLatWGS84 * COORDINATES_SCALING_FACTOR), //
				(int) (maxLonWGS84 * COORDINATES_SCALING_FACTOR), //
				null, 0, nodes, new ArrayList<Node>(), ways, relations);
	}

	public VectorTile(int minLat, int minLon, int maxLat, int maxLon, //
			TagDecoder localDecoder, int nodeEncodedWays, //
			List<Node> nodes, List<Node> ghostNodes, List<Way> ways, List<Relation> relations) {
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.decoder = localDecoder;
		this.nodeEncodedWays = nodeEncodedWays;
		this.nodes = nodes;
		this.ghostNodes = ghostNodes;
		this.ways = ways;
		this.relations = relations;
	}

	public Node getNode(int nId) {
		if (nId < nodes.size()) {
			return nodes.get(nId);
		}
		return ghostNodes.get(nId - nodes.size());
	}

	public Way getWay(int i) {
		return ways.get(i);
	}

	public boolean isGhostNode(int nId) {
		return nId >= nodes.size();
	}

	public Relation getRelation(int i) {
		return relations.get(i);
	}

	public String debugDecodedWays() {
		String result = "";
		for (int i = 0; i < nodeEncodedWays; i++) {
			Way w = getWays().get(i);
			for (Long nid : w.getNodes()) {
				result += nid + " ";
			}
			result += "\n";
		}
		return result;
	}

	public String toString(TagDecoder td) {
		String result = "Vector tile (" + minLat + ", " + minLon + "; " + maxLat + ", " + maxLon + "):\n";
		for (int i = 0; i < nodes.size(); i++) {
			result += i + ": " + nodes.get(i).toString(td);
		}
		for (int i = 0; i < ways.size(); i++) {
			result += i + ": " + ways.get(i).toString(td);
		}
		for (int i = 0; i < relations.size(); i++) {
			result += i + ": " + relations.get(i).toString(td);
		}

		return result;
	}

	public int getMaxLat() {
		return maxLat;
	}

	public int getMaxLon() {
		return maxLon;
	}

	public int getMinLat() {
		return minLat;
	}

	public int getMinLon() {
		return minLon;
	}

	public double getMaxLatWGS84() {
		return ((double) maxLat) / COORDINATES_SCALING_FACTOR;
	}

	public double getMaxLonWGS84() {
		return ((double) maxLon) / COORDINATES_SCALING_FACTOR;
	}

	public double getMinLatWGS84() {
		return ((double) minLat) / COORDINATES_SCALING_FACTOR;
	}

	public double getMinLonWGS84() {
		return ((double) minLon) / COORDINATES_SCALING_FACTOR;
	}

	public int WGS84toNodeLat(double lat) {
		return (int) ((lat - getMinLatWGS84()) * Node.NODE_SCALING_FACTOR);
	}

	public int WGS84toNodeLon(double lon) {
		return (int) ((lon - getMinLonWGS84()) * Node.NODE_SCALING_FACTOR);
	}

	public int getNodeEncodedWays() {
		return nodeEncodedWays;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Way> getWays() {
		return ways;
	}

	public List<Relation> getRelations() {
		return relations;
	}

	public TagDecoder getDecoder() {
		return decoder;
	}

	public List<Node> getGhostNodes() {
		return ghostNodes;
	}

}
