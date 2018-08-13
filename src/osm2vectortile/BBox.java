package osm2vectortile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vectortile.TagDecoder;
import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.optimizers.TagAnalyzer;
import vectortile.optimizers.VectorTileReshuffler;
import vectortile.tools.MultiRemapper;

public class BBox {

	/**
	 * Coordinates in (WGS84*1000)-(reference coordinate)
	 */
	private final int minlat, maxlat, minlon, maxlon;
	private final RawData rawData;
	private final List<Node> ghostNodes = new ArrayList<>();

	public BBox(RawData rawData, int minlat, int minlon, int maxlat, int maxlon) {
		this.rawData = rawData;
		this.minlat = minlat;
		this.maxlat = maxlat;
		this.minlon = minlon;
		this.maxlon = maxlon;

		ghostNodes.add(new Node(minlat, minlon));
		ghostNodes.add(new Node(minlat, maxlon));
		ghostNodes.add(new Node(maxlat, maxlon));
		ghostNodes.add(new Node(maxlat, minlon));
	}

	public VectorTile asVectorTile(boolean clip) {
		/*
		 * TODO clip relations -> For non-multipolygons: check if there are visible
		 * members, if not, drop the relation -> For multipolygons: check if there is a
		 * visible part and include it
		 */
		List<Node> nodes = new ArrayList<>();
		List<Way> ways = new ArrayList<>();

		Map<Long, Long> rewriteNodeIds = new HashMap<>();
		Map<Long, Long> rewriteWayIds = new HashMap<>();
		Map<Long, Long> rewriteRelationIds = new HashMap<>();

		for (Long nId : rawData.getNodes().keySet()) {
			Node n = rawData.getNodes().get(nId);
			if (clip && !isNodeContained(n)) {
				continue;
			}

			long newId = nodes.size();
			rewriteNodeIds.put(nId, newId);
			nodes.add(n);
		}

		for (long wId : rawData.getWays().keySet()) {
			Way w = rawData.getWays().get(wId);
			if (clip) {
				w = clipWay(w);

			}
			if (w == null) {
				continue;
			}
			rewriteWayIds.put(wId, (long) ways.size());
			ways.add(w);
		}

		long i = 0;
		long id = nodes.size();
		for (@SuppressWarnings("unused")
		Node node : ghostNodes) {
			i--;
			rewriteNodeIds.put(i, id);
			id++;
		}

		List<Relation> relations = new ArrayList<>();

		MultiRemapper mr = new MultiRemapper(rewriteNodeIds, rewriteWayIds, rewriteRelationIds);
		mr.apply(ways, relations);

		// More tag analysis!

		TagAnalyzer nodesAnalysis = new TagAnalyzer(nodes);
		nodesAnalysis.recodeAll();
		TagAnalyzer waysAnalysis = new TagAnalyzer(ways);
		waysAnalysis.recodeAll();
		TagAnalyzer relationAnalysis = new TagAnalyzer(relations);
		relationAnalysis.recodeAll();

		TagDecoder localDecoder = new TagDecoder(nodesAnalysis.getLessCommonPairs(), waysAnalysis.getLessCommonPairs(),
				relationAnalysis.getLessCommonPairs());

		VectorTile vt = new VectorTile(minlat, minlon, maxlat, maxlon, localDecoder, 0, nodes, ghostNodes, ways,
				relations);
		return new VectorTileReshuffler(vt).constructShuffledTile();
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
		List<Long> newIds = clipPolyWith(w.getNodes());
		if (newIds == null) {
			return null;
		}

		return new Way(w.getTags(), newIds);
	}

	private List<Long> clipPolyWith(List<Long> newPolygon) {
		List<Long> clipper = new ArrayList<>(4);
		clipper.add(-1l);
		clipper.add(-2l);
		clipper.add(-3l);
		clipper.add(-4l);

		int len = clipper.size();

		for (int i = 0; i < len; i++) {
			int curLen = newPolygon.size();
			List<Long> input = newPolygon;
			newPolygon = new ArrayList<>();

			long A = clipper.get((i + len - 1) % len);
			long B = clipper.get(i);
			for (int j = 0; j < curLen; j++) {

				// Previous point, wraps over
				Long P = input.get((j + curLen - 1) % curLen);
				// Current point
				Long Q = input.get(j);

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

	private boolean isInside(long a, long b, long c) {
		return (getLat(a) - getLat(c)) * (getLon(b) - getLon(c)) > (getLon(a) - getLon(c)) * (getLat(b) - getLat(c));
	}

	private double getLat(long index) {
		return getFusedNode(index).getLat();
	}

	private double getLon(long index) {
		return getFusedNode(index).getLon();
	}

	private Node getFusedNode(long index) {
		if (index < 0) {
			return ghostNodes.get((int) (-index - 1));
		}
		return rawData.getNodes().get(index);
	}

	private long intersection(long a, long b, long p, long q) {
		double A1 = getLon(b) - getLon(a);
		double B1 = getLat(a) - getLat(b);
		double C1 = A1 * getLat(a) + B1 * getLon(a);

		double A2 = getLon(q) - getLon(p);
		double B2 = getLat(p) - getLat(q);
		double C2 = A2 * getLat(p) + B2 * getLon(p);

		double det = A1 * B2 - A2 * B1;
		double x = (B2 * C1 - B1 * C2) / det;
		double y = (A1 * C2 - A2 * C1) / det;
		Node n = new Node((int) x, (int) y);
		ghostNodes.add(n);
		return 0l - ghostNodes.size();
	}

	@Override
	public String toString() {
		return "BBOX " + minlat + ", " + minlon + "; " + maxlat + ", " + maxlon;
	}

}
