package vectortile.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vectortile.TagDecoder;
import vectortile.Types;

/**
 * A compact representation of the full tagsset for objects.
 * 
 * Note that these are compacted based on context: - Globally common tags are
 * encoded with a number (e.g.'building=yes' is number '0' for ways) - Within
 * the vectortile, all tags are grouped similarly as well. - During
 * constructions (before the local tag table is built), otherTags will be
 * populated as well.
 */
public class Tags {

	private final List<Integer> commonTags;
	private final List<Integer> lessCommonTags; // Tags that turned out to be common locally
	private final List<Tag> otherTags; // NOT Serialized
	private List<Integer> memberOfRelations; // Mainly used for rendering. Repopulated during serialization

	private final static ArrayList<Integer> EMPTY = new ArrayList<>();
	
	public Tags(String... tags) {
		commonTags = EMPTY;
		lessCommonTags = EMPTY;
		otherTags = new ArrayList<>();
		for (int i = 0; i < tags.length; i+=2) {
			otherTags.add(new Tag(tags[i], tags [i+1]));
		}
	}
	
	public Tags(List<Tag> tags) {
		this(EMPTY, EMPTY, tags);
	}
	
	public Tags(List<Integer> commonTags, List<Integer> lessCommonTags, List<Tag> othertags) {
		this.commonTags = commonTags;
		this.lessCommonTags = lessCommonTags;
		this.otherTags = othertags;
		Collections.sort(commonTags);
		Collections.sort(lessCommonTags);

	}

	public boolean contains(Types type, TagDecoder global, TagDecoder td, String key, String value) {
		Tag t = new Tag(key, value);
		Integer commonTag = global.encode(type, t);
		if (commonTag != null) {
			int index = Collections.binarySearch(commonTags, commonTag);
			if (index >= 0) {
				return true;
			}
		}
		if (td != null) {
			Integer otherTag = td.encode(type, t);
			if (otherTag != null) {
				int index = Collections.binarySearch(lessCommonTags, otherTag);
				if (index >= 0) {
					return true;
				}
			}
		}
		return this.otherTags.contains(t);
	}
	

	@Override
	public String toString() {
		String tags = "";
		for (int i : commonTags) {
			tags += i + ", ";
		}
		for (int i : lessCommonTags) {
			tags += i + ", ";
		}
		for (Tag pair : otherTags) {
			tags += pair + ", ";
		}

		return "{" + tags + "}";
	}

	public String toString(Types t, TagDecoder td, TagDecoder local) {

		String tags = "";
		for (int i : commonTags) {
			tags += "  " + td.decode(t, i) + "(common tag #" + i + ")\n";
		}
		for (int i : lessCommonTags) {
			tags += "  " + local.decode(t, i) + "(lcommon tag #" + i + ")\n";
		}
		for (Tag pair : otherTags) {
			tags += "  " + pair + "\n";
		}

		return "{\n" + tags + "}\n";
	}
	
	public int getCount() {
		return lessCommonTags.size() + commonTags.size() + otherTags.size();
	}


	public List<Integer> getMemberOfRelations() {
		return memberOfRelations;
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
	
	public Tags addOther(Tag t) {
		List<Tag> newTags = new ArrayList<>(otherTags);
		newTags.add(t);
		return new Tags(commonTags, lessCommonTags, newTags);
	}

	public String getValueOf(Types t, TagDecoder global, TagDecoder local, String key) {
		Set<Integer> tagsWithKey = new HashSet<>(global.tagsWithKey(t, key));
		for (int commonTag : commonTags) {
			if(tagsWithKey.contains(commonTag)) {
				return global.decode(t, commonTag).value;
			}
		}
		
		tagsWithKey = new HashSet<>(local.tagsWithKey(t, key));
		for (int lesscommonTag : lessCommonTags) {
			if(tagsWithKey.contains(lesscommonTag)) {
				return local.decode(t, lesscommonTag).value;
			}
		}
		
		return null;
	}


}
