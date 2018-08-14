package vectortile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import vectortile.data.Tag;
import vectortile.data.Taggable;
import vectortile.data.Tags;

public class TagDecoder {

	private final List<Tag> nodeTags, waytags, relationTags;

	public TagDecoder(List<Tag> nodeTags, List<Tag> waytags, List<Tag> relationTags) {
		this.nodeTags = nodeTags;
		this.waytags = waytags;
		this.relationTags = relationTags;
	}

	public TagDecoder(String fileLocation) throws IOException {
		this(buildDecodeTable(fileLocation + "/popular_node_tags.csv"), //
				buildDecodeTable(fileLocation + "/popular_way_tags.csv"), //
				buildDecodeTable(fileLocation + "/popular_relation_tags.csv"));
	}

	public TagDecoder() throws IOException {
		this("res");
	}

	public void encodeAll(Types type, List<? extends Taggable> taggables, boolean globalTags) {
		for (Taggable t : taggables) {
			t.setTags(buildTagsFor(type, t.getTags(), globalTags));
		}
	}

	private Tags buildTagsFor(Types t, Tags tags, boolean globalTags) {
		List<Integer> commonTags = new ArrayList<>();
		List<Integer> lessCommonTags = new ArrayList<>();
		List<Tag> otherTags = new ArrayList<>();

		for (Tag pair : tags.getOtherTags()) {
			Integer encoded = encode(t, pair);
			if (encoded != null) {
				if (globalTags) {
					commonTags.add(encoded);
				} else {
					lessCommonTags.add(encoded);
				}
			} else {
				otherTags.add(pair);
			}
		}
		return new Tags(commonTags, lessCommonTags, otherTags);
	}

	/**
	 * Will be null if not a common tag!
	 */
	public Integer encode(Types t, Tag p) {
		List<Tag> pairs = selectTable(t);
		for (int i = 0; i < pairs.size(); i++) {
			if (p.equals(pairs.get(i))) {
				return i;
			}
		}
		return null;
	}

	public Tag decode(Types t, int code) {
		return selectTable(t).get(code);
	}

	private List<Tag> selectTable(Types t) {
		switch (t) {
		case NODE:
			return nodeTags;
		case WAY:
			return waytags;
		case RELATION:
			return relationTags;
		default:
			throw new IllegalStateException("Should never happen, or did OSM introduce a new basetype?");
		}
	}

	private final static List<Tag> buildDecodeTable(String path) throws IOException {
		return buildDecodeTable(Files.readAllLines(Paths.get(path)));
	}

	private final static List<Tag> buildDecodeTable(List<String> data) {
		List<Tag> decodedationTable = new ArrayList<>();
		for (String line : data) {
			String[] parts = line.split(",");
			decodedationTable.add(new Tag(parts[0], parts[1]));
		}
		return decodedationTable;
	}

	public List<Tag> getNodeTags() {
		return nodeTags;
	}

	public List<Tag> getWaytags() {
		return waytags;
	}

	public List<Tag> getRelationTags() {
		return relationTags;
	}
}
