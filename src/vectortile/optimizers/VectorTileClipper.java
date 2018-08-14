package vectortile.optimizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import utils.Utils;
import vectortile.Types;
import vectortile.data.Member;
import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.tools.MultiRemapper;

public class VectorTileClipper extends Optimizer {

	private final VectorTile vt;
	private final List<Node> ghostNodes = new ArrayList<>();

	// IN vector tile coordinates
	private final int minlat, maxlat, minlon, maxlon;

	public VectorTileClipper(VectorTile vt) {
		this(vt, vt.getMinLatWGS84(), vt.getMinLonWGS84(), vt.getMaxLatWGS84(), vt.getMaxLonWGS84());
	}

	public VectorTileClipper(VectorTile vectorTile,
			// Cut along points
			double minLatWGS84, double minLonWGS84, double maxLatWGS84, double maxLonWGS84) {
		this.vt = vectorTile;

		this.minlat = vt.WGS84toNodeLat(minLatWGS84);
		this.minlon = vt.WGS84toNodeLon(minLonWGS84);
		this.maxlat = vt.WGS84toNodeLat(maxLatWGS84);
		this.maxlon = vt.WGS84toNodeLon(maxLonWGS84);

		ghostNodes.add(new Node(minlat, minlon, null));
		ghostNodes.add(new Node(minlat, maxlon, null));
		ghostNodes.add(new Node(maxlat, maxlon, null));
		ghostNodes.add(new Node(maxlat, minlon, null));
	}

	@Override
	public VectorTile createOptimized() {
		List<Node> newNodes = new ArrayList<>();
		Map<Long, Long> nodeRemapping = copyContainedNodes(newNodes);

		List<Way> newWays = new ArrayList<>();
		Map<Long, Long> wayRemapping = new HashMap<>();
		for (int i = 0; i < vt.getWays().size(); i++) {
			Way w = clipWay(vt.getWay(i));
			if (w == null) {
				// Clipped away
				continue;
			}
			wayRemapping.put((long) i, (long) newWays.size());
			newWays.add(w);
		}
		
		addGhostNodeIndexes(nodeRemapping, newNodes.size());

		// Having a look at the relations: we remove all the members that are not there
		// anymore
		// A member (e.g. a node) has been pruned if its key is missing in the remapping
		List<Relation> newRelations = new ArrayList<>();
		Map<Long, Long> relationRemapping = new HashMap<>();
		Map<Long, Relation> metaRelations = new HashMap<>();

		for (int i = 0; i < vt.getRelations().size(); i++) {
			Relation relation = vt.getRelation(i);
			Relation pruned = pruneRelation(relation, nodeRemapping, wayRemapping);
			if (pruned == null) {
				continue;
			}
			relationRemapping.put((long) i, (long) newRelations.size());
			newRelations.add(pruned);

			if (relation.containsRelation()) {
				metaRelations.put((long) i, relation);
			}

		}

		boolean relationDropped = true;
		while (relationDropped) {
			relationDropped = false;
			for (Long key : metaRelations.keySet()) {
				Relation relation = metaRelations.get(key);
				filterUsed(relation, relationRemapping);
				if (relation.getMembers().size() == 0) {
					relationRemapping.remove(key);
					metaRelations.remove(key);
					relationDropped = true;
				}
			}
		}

		MultiRemapper mr = new MultiRemapper(nodeRemapping, wayRemapping, relationRemapping);
		mr.apply(newWays, newRelations);

		return new VectorTile(minlat, minlon, maxlat, maxlon, //
				vt.getDecoder(), 0, //
				newNodes, ghostNodes, newWays, newRelations);
	}
	
	private void addGhostNodeIndexes(Map<Long, Long> nodeRemapping, int startId) {
		for (int i = 0; i < this.ghostNodes.size(); i++) {
			nodeRemapping.put(-1l - i, (long) startId);
			startId++;
		}
	}

	private static void filterUsed(Relation r, Map<Long, Long> knownRelations) {
		for (Iterator<Member> iterator = r.getMembers().iterator(); iterator.hasNext();) {
			Member m = iterator.next();
			if (m.type != Types.RELATION) {
				continue;
			}

			if (knownRelations.containsKey(m.index)) {
				continue;
			}

			// The relation this member refers to has gone
			iterator.remove();
		}
	}

	private static Relation pruneRelation(Relation r, Map<Long, Long> nodeRemapping, Map<Long, Long> wayRemapping) {
		List<Member> newMembers = new ArrayList<>();
		for (Member m : r.getMembers()) {
			switch (m.type) {
			case NODE:
				if (nodeRemapping.containsKey(m.index)) {
					newMembers.add(m);
				}
				break;
			case WAY:
				if (wayRemapping.containsKey(m.index)) {
					newMembers.add(m);
				}
				break;
			case RELATION:
				newMembers.add(m);
				break;
			}
		}
		if (newMembers.isEmpty()) {
			return null;
		}
		return new Relation(newMembers, r.getTags());

	}

	public Map<Long, Long> copyContainedNodes(List<Node> toAdd) {
		Map<Long, Long> remapping = new HashMap<>();
		for (int i = 0; i < vt.getNodes().size(); i++) {
			Node n = vt.getNode(i);
			if (!isNodeContained(n)) {
				continue;
			}
			remapping.put((long) i, (long) toAdd.size());
			toAdd.add(n);
		}
		return remapping;
	}

	public boolean isNodeContained(Node n) {
		return (minlat <= n.lat && n.lat <= maxlat) && (minlon <= n.lon && n.lon <= maxlon);
	}

	/**
	 * Creates a copy of the way w, which is clipped. Takes a function which creates
	 * new Nodes and gives an identifier for them
	 * 
	 * @param w
	 */
	public Way clipWay(Way w) {
		List<Integer> newIds = clipPolyWith(Utils.longList2intList(w.getNodes()));
		if (newIds == null) {
			return null;
		}

		return new Way(w.getTags(), Utils.intList2longList(newIds), w.getCenter());
	}

	private List<Integer> clipPolyWith(List<Integer> newPolygon) {
		List<Integer> clipper = new ArrayList<>(4);
		clipper.add(-1);
		clipper.add(-2);
		clipper.add(-3);
		clipper.add(-4);

		int len = clipper.size();

		for (int i = 0; i < len; i++) {
			int curLen = newPolygon.size();
			List<Integer> input = newPolygon;
			newPolygon = new ArrayList<>();

			int A = clipper.get((i + len - 1) % len);
			int B = clipper.get(i);
			for (int j = 0; j < curLen; j++) {

				// Previous point, wraps over
				int P = input.get((j + curLen - 1) % curLen);
				// Current point
				int Q = input.get(j);

				if ((j == 0 || j == curLen) && isNodeContained(getFusedNode(Q))) {
					// The first and last element should not wrap around. If it is contained within
					// the BBOX, we're fine
					newPolygon.add(Q);
					continue;
				}

				if (!isInside(A, B, Q)) { // is Q inside of Nodes A and B
					if (isInside(A, B, P)) { // is P outside of A and B
						newPolygon.add(intersection(A, B, P, Q));
					}
					newPolygon.add(Q);
				} else if (!isInside(A, B, P)) // is P inside of A and B and Q outside of
					newPolygon.add(intersection(A, B, P, Q));
			}
		}

		if (newPolygon.isEmpty()) {
			return null;
		}

		return newPolygon;
	}

	private boolean isInside(int a, int b, int c) {
		return (getLat(a) - getLat(c)) * (getLon(b) - getLon(c)) > (getLon(a) - getLon(c)) * (getLat(b) - getLat(c));
	}

	private double getLat(int index) {
		return getFusedNode(index).getLat();
	}

	private double getLon(int index) {
		return getFusedNode(index).getLon();
	}

	private Node getFusedNode(int index) {
		if (index < 0) {
			return ghostNodes.get((int) (-index - 1));
		}
		return vt.getNode(index);
	}

	private int intersection(int a, int b, int p, int q) {
		double A1 = getLon(b) - getLon(a);
		double B1 = getLat(a) - getLat(b);
		double C1 = A1 * getLat(a) + B1 * getLon(a);

		double A2 = getLon(q) - getLon(p);
		double B2 = getLat(p) - getLat(q);
		double C2 = A2 * getLat(p) + B2 * getLon(p);

		double det = A1 * B2 - A2 * B1;
		double x = (B2 * C1 - B1 * C2) / det;
		double y = (A1 * C2 - A2 * C1) / det;
		Node n = new Node((int) x, (int) y, null);
		ghostNodes.add(n);
		return 0 - ghostNodes.size();
	}

	@Override
	public String toString() {
		return "BBOX " + minlat + ", " + minlon + "; " + maxlat + ", " + maxlon;
	}

}
