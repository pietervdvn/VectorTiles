package vectortile.optimizers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.VectorTile;
import vectortile.data.Way;

public class TagCompactor extends Optimizer {

	private final VectorTile vt;

	public TagCompactor(VectorTile vectorTile) {
		this.vt = vectorTile;
	}

	@Override
	public VectorTile createOptimized() {

		List<Node> newNodes = new ArrayList<>(vt.getNodes());
		List<Way> newWays = new ArrayList<>(vt.getWays());
		List<Relation> newRelations = new ArrayList<>(vt.getRelations());

		try {
			TagDecoder globalDecoder = new TagDecoder();
			globalDecoder.encodeAll(Types.NODE, newNodes, true);
			globalDecoder.encodeAll(Types.WAY, newWays, true);
			globalDecoder.encodeAll(Types.RELATION, newRelations, true);

		} catch (IOException e) {
			System.err.println("Could not load lists with most common tags");
		}

		TagAnalyzer nd = new TagAnalyzer(newNodes);
		TagAnalyzer wy = new TagAnalyzer(vt.getWays());
		TagAnalyzer rl = new TagAnalyzer(vt.getRelations());

		TagDecoder localDecoder = new TagDecoder(nd.getLessCommonPairs(), wy.getLessCommonPairs(),
				rl.getLessCommonPairs());
		localDecoder.encodeAll(Types.NODE, newNodes, false);
		localDecoder.encodeAll(Types.WAY, newWays, false);
		localDecoder.encodeAll(Types.RELATION, newRelations, false);

		return new VectorTile(vt.getMinLat(), vt.getMinLon(), vt.getMaxLat(), vt.getMaxLon(), //
				localDecoder, vt.getNodeEncodedWays(), newNodes, vt.getGhostNodes(), newWays, newRelations);

	}

}
