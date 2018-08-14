package vectortile.optimizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.tools.MultiRemapper;

/**
 * Reshuffles the nodes and ways to create a new Vector tile which allows
 * nodeEncoded ways
 * 
 * (When writing the nodelist to file, we order the first N nodes to be the
 * first nodes of way W. Then, we write the nodes for W2, ... )
 */
public class VectorTileReshuffler extends Optimizer {

	private final VectorTile vt;
	/*
	 * Working copy of the nodes- and relationslists. Will be modified, hence the
	 * copy
	 */
	private final List<Node> nodes;
	private final List<Relation> relations;
	private final List<Way> ways;

	private final Map<Long, Long> wayRemapping = new HashMap<>();
	private final Map<Long, Long> nodeRemapping = new HashMap<>();

	private final List<Way> newWays = new ArrayList<>();
	private final List<Node> newNodes = new ArrayList<>();

	public VectorTileReshuffler(VectorTile vectorTile) {
		this.vt = vectorTile;

		this.nodes = new ArrayList<>(vt.getNodes());
		this.relations = new ArrayList<>(vt.getRelations());
		this.ways = new ArrayList<>(vt.getWays());
	}

	@Override
	public VectorTile createOptimized() {
		// Sort the ways to get the longest ways upfront
		ways.sort(Way.LENGTH_COMPARATOR_DESC);

		int nodeEncoded = 0;
		for (int i = 0; i < ways.size(); i++) {
			Way way = ways.get(i);
			if (!eligibleForEncoding(way)) {
				continue;
			}

			for (long nId : way.getNodes()) {
				int nid = (int) nId;
				if (nodes.get(nid) == null) {
					continue; // we have already seen this node, prolly as startpoint
				}
				nodeRemapping.put(nId, (long) newNodes.size());
				newNodes.add(nodes.get(nid));
				nodes.set(nid, null);
			}

			wayRemapping.put((long) i, (long) newWays.size());
			newWays.add(way);
			ways.set(i, null);
			nodeEncoded++;
		}

		// copy the leftovers
		copyLeftovers(ways, newWays, wayRemapping);
		copyLeftovers(nodes, newNodes, nodeRemapping);

		for (int i = 0; i < vt.getGhostNodes().size(); i++) {
			long kv = nodes.size() + i; //
			// Add identity mapping for ghost nodes
			nodeRemapping.put(kv, kv);
		}

		// At last, change the indices everywhere
		MultiRemapper mr = new MultiRemapper(nodeRemapping, wayRemapping, new HashMap<>());
		mr.apply(newWays, relations);

		// And get the new vectortile out!
		return new VectorTile(vt.getMinLat(), vt.getMinLon(), vt.getMaxLat(), vt.getMaxLon(), //
				vt.getDecoder(), nodeEncoded, newNodes, vt.getGhostNodes(), newWays, relations);
	}

	private boolean eligibleForEncoding(Way way) {
		List<Long> wayNodes = way.getNodes();
		if (wayNodes.get(0) != wayNodes.get(wayNodes.size() - 1)) {
			// Only closed nodes are allowed for way-encoding
			return false;
		}

		Set<Long> nodesSeen = new HashSet<Long>();
		for (int i = 0; i < wayNodes.size(); i++) {
			long nId = wayNodes.get(i);
			int nid = (int) nId;

			if (vt.isGhostNode(nid)) {
				// Can't do this with ghost nodes for now
				return false;
			}

			if (nodes.get(nid) == null) {
				// Already used in another way, already moved
				return false;
			}

			if (nodesSeen.contains(nId) && !(i == wayNodes.size() - 1 && nId == wayNodes.get(0))) {
				// The way reuses the same node multiple times
				// Let's not encode it...
				// Except for the last element of course, it should equal the first
				return false;
			}
			nodesSeen.add(nId);
		}

		return true;
	}

	private static <T> void copyLeftovers(List<T> source, List<T> sink, Map<Long, Long> remapping) {
		for (int i = 0; i < source.size(); i++) {
			T n = source.get(i);
			if (n == null) {
				// already in new list
				continue;
			}
			remapping.put((long) i, (long) sink.size());
			sink.add(n);
		}
	}

}
