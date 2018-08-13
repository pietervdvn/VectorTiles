package vectortile.serialization;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.data.Node;
import vectortile.data.Tag;
import vectortile.data.Tags;
import vectortile.data.VectorTile;
import vectortile.data.Way;

public class BinaryDecoder {

	private final static List<Tag> EMPTY = new ArrayList<>();
	private final DataInputStream in;

	public BinaryDecoder(InputStream in) {
		this.in = new DataInputStream(in);
	}

	
	public VectorTile deserialize() throws IOException {

		// Start with reading metainformation: bounds
		int minLat = in.readInt();
		int minLon = in.readInt();
		int maxLat = in.readInt();
		int maxLon = in.readInt();

		TagDecoder localDecoder = readLessCommonTagTable();
		List<Node> nodes = readNodes();
		List<Node> ghostNodes = readNodes();
		readNodeTagging(nodes);

		List<Way> ways = readWays();

		return new VectorTile(minLat, minLon, maxLat, maxLon, localDecoder, 0, nodes, ghostNodes, ways, null);
	}

	private TagDecoder readLessCommonTagTable() throws IOException {
		return new TagDecoder(readPairs(), readPairs(), readPairs());
	}

	private List<Way> readWays() throws IOException {
		int l = in.readInt();
		int nodeEncoded = in.readInt();
		List<Way> ways = new ArrayList<>(l + nodeEncoded);

		int nodeCounter = 0;
		for (int i = 0; i < nodeEncoded; i++) {
			int s = in.readInt();
			List<Long> nodeIds = new ArrayList<>(s + 1);
			for (int j = 0; j < s; j++) {
				nodeIds.add((long) nodeCounter);
				nodeCounter++;
			}
			nodeIds.set(s, nodeIds.get(0));
			ways.add(new Way(readTags(), nodeIds));
		}

		for (int count = 0; count < l; count++) {
			int ll = in.readInt();

			List<Long> nodeIds = new ArrayList<>(ll);
			for (int i = 0; i < ll; i++) {
				nodeIds.add((long) in.readInt());
			}
			Tags t = readTags();
			Way w = new Way(t, nodeIds);
			ways.add(w);
		}
		return ways;
	}

	private List<Node> readNodes() throws IOException {
		// Writing the normal nodes
		int l = in.readInt();
		List<Node> nodes = new ArrayList<>(l);
		for (int i = 0; i < l; i++) {
			int lat = in.readInt();
			int lon = in.readInt();
			nodes.add(new Node(lat, lon));
		}
		return nodes;
	}


	private void readNodeTagging(List<Node> nodes) throws IOException {
		int taggedNodes = in.readInt();
		for (int count = 0; count < taggedNodes; count++) {
			int i = in.readInt();
			Node n = nodes.get(i);
			Tags t = readTags();
			n.setTags(t);
		}
	}

	private Tags readTags() throws IOException {
		/**
		 * First we read the number of common tags (byte), followed by those common tags
		 * (bytes) After this, we read (as strings) the other tags.
		 */
		int length = in.readInt();
		List<Integer> commonTags = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			commonTags.add((int) in.readByte());
		}

		length = in.readInt();
		List<Integer> lessCommonTags = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			lessCommonTags.add((int) in.readByte());
		}

		// List<Pair> otherTags = readPairs(in);
		// All tags are encoded as lessCommontags
		return new Tags(commonTags, lessCommonTags, EMPTY);
	}

	private List<Tag> readPairs() throws IOException {
		int length = in.readInt();
		List<Tag> pairs = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			pairs.add(new Tag(in.readUTF(), in.readUTF()));
		}
		return pairs;
	}
}
