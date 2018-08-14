package vectortile.serialization;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.data.Node;
import vectortile.data.Tag;
import vectortile.data.Tags;
import vectortile.data.VectorTile;
import vectortile.data.Way;

public class BinaryEncoder {

	private final VectorTile vt;

	public BinaryEncoder(VectorTile vt) {
		this.vt = vt;
	}

	public void serialize(String path) throws IOException {
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(path)))) {
			serialize(out);
		}
	}

	public void serialize(OutputStream os) throws IOException {

		/**
		 * The file format: We start with vt.lat and vt.lon (int * 1000) This is
		 * followed by the number of nodes (int), followed by (lat, lon) pairs (note:
		 * lat/lon in diffed and multiplied ints). Tags for nodes are written
		 * aftwerwards, followed by ways and relations
		 */
		DataOutputStream out = new DataOutputStream(os);

		// Start with writing metainformation: bounds
		out.writeInt(vt.getMinLat());
		out.writeInt(vt.getMinLon());

		out.writeInt(vt.getMaxLat());
		out.writeInt(vt.getMaxLon());

		
		boolean compactedTags = vt.getDecoder() != null;
		out.writeBoolean(compactedTags);
		if(compactedTags) {
			writeLessCommonTagTable(out, vt.getDecoder());
		}
		int taggedNodes = writeNodes(out, vt.getNodes());
		writeNodes(out, vt.getGhostNodes());
		writeNodeTagging(out, vt.getNodes(), taggedNodes, compactedTags);

		writeWays(out, vt.getNodeEncodedWays(), vt.getWays(), compactedTags);


		System.out.println("Done! Written " + out.size() + " bytes");
	}

	private static void writeLessCommonTagTable(DataOutputStream out, TagDecoder td) throws IOException {
		writeTagList(out, td.getNodeTags());
		writeTagList(out, td.getWaytags());
		writeTagList(out, td.getRelationTags());
	}

	private static void writeWays(DataOutputStream out, int nodeEncoded, List<Way> ways, boolean compactedTags) throws IOException {
		out.writeInt(ways.size() - nodeEncoded);
		out.writeInt(nodeEncoded);

		for (int i = 0; i < nodeEncoded; i++) {
			Way w = ways.get(i);
			out.writeInt(w.getNodes().size() - 1); // All node-encoded ways are closed
			writeTags(out, w.getTags(), compactedTags);
		}

		for (int i = nodeEncoded; i < ways.size(); i++) {
			Way w = ways.get(i);
			List<Long> nodeIds = w.getNodes();
			out.writeInt(nodeIds.size());
			for (long nodeId : nodeIds) {
				out.writeInt((int) nodeId);
			}
			writeTags(out, w.getTags(), compactedTags);
		}
	}

	private static int writeNodes(DataOutputStream out, List<Node> nodes) throws IOException {
		// Writing the normal nodes
		out.writeInt(nodes.size());

		int taggedNodes = 0; // in the meanwhile, we count the number of tagged nodes
		for (Node n : nodes) {
			out.writeInt(n.lat);
			out.writeInt(n.lon);
			if (n.getTags() != null && n.getTags().getCount() > 0) {
				taggedNodes++;
			}
		}
		return taggedNodes;
	}

	private static void writeNodeTagging(DataOutputStream out, List<Node> nodes, int taggedNodes, boolean compactedTags) throws IOException {
		out.writeInt(taggedNodes);
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			if (n.getTags() == null || n.getTags().getCount() == 0) {
				continue;
			}
			out.writeInt(i);
			writeTags(out, n.getTags(), compactedTags);
		}
	}

	private static void writeTags(DataOutputStream out, Tags t, boolean compactedTags) throws IOException {
		if (t == null || t.getCount() == 0) {
			out.writeInt(0);
			out.writeInt(0);
			return;
		}
		/**
		 * First we write the number of common tags (byte), followed by those common
		 * tags (bytes) After this, we write (as strings) the other tags.
		 */
		out.writeInt(t.getCommonTags().size());
		for (int i : t.getCommonTags()) {
			out.writeByte(i);
		}

		out.writeInt(t.getLessCommonTags().size());
		for (int i : t.getLessCommonTags()) {
			out.writeInt(i);
		}

		if(!compactedTags) {
			writeTagList(out, t.getOtherTags());
		}else if (t.getOtherTags().size() > 0) {
			throw new IllegalStateException("All pairs should be encoded at this point");
		}
		
		
	}

	private static void writeTagList(DataOutputStream out, List<Tag> pairs) throws IOException {
		out.writeInt(pairs.size());
		for (Tag p : pairs) {
			out.writeUTF(p.key);
			out.writeUTF(p.value);
		}
	}

}
