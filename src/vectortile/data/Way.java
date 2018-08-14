package vectortile.data;

import java.util.Comparator;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.tools.Remapper;

public class Way extends Taggable {

	private final List<Long> nodes;
	private Node center;

	public Way(Tags tags, List<Long> nodes, Node center) {
		super(tags);
		this.nodes = nodes;
		this.center = center;
	}

	public void remap(Remapper rewrite) {
		rewrite.rewriteAll(nodes);
	}

	public String toString(TagDecoder td, TagDecoder local) {
		return this.toString() + super.toString(Types.WAY, td, local);
	}

	@Override
	public String toString() {
		String result = "way ";
		for (Long nid : nodes.subList(0, Math.min(3, nodes.size()))) {
			result += nid + ", ";

		}
		return result;
	}

	public List<Long> getNodes() {
		return nodes;
	}

	public Node getCenter() {
		return center;
	}

	public void setCenter(Node center) {
		this.center = center;
	}

	public void calculateCenter(List<Node> allNodes) {
		int lat = 0, lon = 0;
		for (long l : nodes) {
			lat += allNodes.get((int) l).getLat();
			lon += allNodes.get((int) l).getLon();
		}
		this.center = new Node(lat / nodes.size(), lon / nodes.size(), null);
	}

	public static final Comparator<Way> LENGTH_COMPARATOR_DESC = new Comparator<Way>() {
		@Override
		public int compare(Way o1, Way o2) {
			return o2.getNodes().size() - o1.getNodes().size();
		}
	};

	public static final Comparator<Way> LENGTH_COMPARATOR_ASC = new Comparator<Way>() {
		@Override
		public int compare(Way o1, Way o2) {
			return o1.getNodes().size() - o2.getNodes().size();
		}
	};

}
