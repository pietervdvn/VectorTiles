package vectortile.tools;

import java.util.List;
import java.util.Map;

import vectortile.Types;
import vectortile.data.Relation;
import vectortile.data.Way;

public class MultiRemapper {

	private final Remapper nodesRemap;
	private final Remapper wayRemap;
	private final Remapper relationRemap;

	public MultiRemapper(Remapper nodesRemap, Remapper wayRemap, Remapper relationRemap) {
		this.nodesRemap = nodesRemap;
		this.wayRemap = wayRemap;
		this.relationRemap = relationRemap;
	}

	public MultiRemapper(Map<Long, Long> rewriteNodeIds, Map<Long, Long> rewriteWayIds,
			Map<Long, Long> rewriteRelationIds) {
		this(new Remapper(rewriteNodeIds), new Remapper(rewriteWayIds), new Remapper(rewriteRelationIds));
	}

	public Remapper getRewritor(Types t) {
		switch (t) {
		case NODE:
			return nodesRemap;
		case RELATION:
			return relationRemap;
		case WAY:
			return wayRemap;
		default:
			return null;
		}
	}

	public void apply(List<Way> ways, List<Relation> rels) {
		for (Way w : ways) {
			w.remap(nodesRemap);
		}
		if (rels == null) {
			return;
		}
		for (Relation r : rels) {
			for (int i = 0; i < r.members.length; i++) {
				r.members[i].remap(this);
			}
		}
	}
	
}
