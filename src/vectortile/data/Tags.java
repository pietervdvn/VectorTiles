package vectortile.data;

import java.util.Collections;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;

/**
 * A compact representation of the full tagsset for objects.
 * 
 * Note that these are compacted based on context:
 * - Globally common tags are encoded with a number (e.g.'building=yes' is number '0' for ways)
 * - Within the vectortile, all tags are grouped similarly as well.
 * - During constructions (before the local tag table is built), otherTags will be populated as well.
 */
public class Tags {


	private final List<Integer> commonTags;
	private final List<Integer> lessCommonTags; // Tags that turned out to be common locally
	private final List<Tag> otherTags; // NOT Serialized
	private List<Integer> memberOfRelations; // Mainly used for rendering. Repopulated during serialization

	public Tags(List<Integer> commonTags, List<Integer> lessCommonTags, List<Tag> othertags) {
		this.commonTags = commonTags;
		this.lessCommonTags = lessCommonTags;
		this.otherTags = othertags;
		for (Integer integer : lessCommonTags) {
			if (integer == null) {
				throw new NullPointerException("Null element in lessCommonTags");
			}
		}
		for (Integer integer : commonTags) {
			if (integer == null) {
				throw new NullPointerException("Null element in commontaglist");
			}
		}
		Collections.sort(commonTags);
		Collections.sort(lessCommonTags);

	}

	public List<Integer> getCommonTags() {
		return commonTags;
	}

	public List<Integer> getLessCommonTags() {
		return lessCommonTags;
	}

	public List<Tag> getOtherTags() {
		return otherTags;
	}

	public int getCount() {
		return lessCommonTags.size() + commonTags.size() + otherTags.size();
	}

	@Override
	public String toString() {
		String tags = "";
		for (int i : commonTags) {
			tags += i + ", ";
		}
		for (Tag pair : otherTags) {
			tags += pair + ", ";
		}

		return "{" + tags + "}";
	}

	public String toString(Types t, TagDecoder td) {

		String tags = "";
		for (int i : commonTags) {
			tags += "  " + td.decode(t, i) + "(common tag #" + i + ")\n";
		}
		for (Tag pair : otherTags) {
			tags += "  " + pair + "\n";
		}

		return "{\n" + tags + "}\n";
	}

	public boolean contains(Types t, TagDecoder global, TagDecoder td, String key, String value) {
		Integer commonTag = global.encode(t, new Tag(key, value));
		if (commonTag != null) {
			int index = Collections.binarySearch(commonTags, commonTag);
			if (index >= 0) {
				return true;
			}
		}
		Integer otherTag = td.encode(t, new Tag(key, value));
		if (otherTag != null) {
			int index = Collections.binarySearch(lessCommonTags, otherTag);
			if (index >= 0) {
				return true;
			}
		}
		return false;
	}
	
	public List<Integer> getMemberOfRelations() {
		return memberOfRelations;
	}

}
