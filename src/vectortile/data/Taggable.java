package vectortile.data;

import vectortile.TagDecoder;
import vectortile.Types;

public abstract class Taggable {

	private Tags tags;

	public Taggable(Tags tags) {
		this.tags = tags;
	}

	public Tags getTags() {
		return tags;
	}

	public void setTags(Tags tags) {
		this.tags = tags;
	}

	public String toString(Types t, TagDecoder td, TagDecoder local) {
		if (tags == null) {
			return "\n";
		}
		return tags.toString(t, td, local);
	}

}
