package vectortile.optimizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utils.Histogram;
import vectortile.data.Tag;
import vectortile.data.Taggable;
import vectortile.data.Tags;

/**
 * The tag analyzer groups the 'lessCommon' tagging pairs and replaces them in a
 * Tags-element. This TagAnalyzer will take in all the tags of a single group
 */
class TagAnalyzer {

	private final Iterable<? extends Taggable> allTags;

	private final List<Tag> lessCommonPairs = new ArrayList<>();

	public TagAnalyzer(Iterable<? extends Taggable> allTags) {
		this.allTags = allTags;

		Histogram<Tag> hist = buildCommonPairs();
		Map<Tag, Integer> lessCommonPairs = hist.getCounts();

		for (Tag pair : lessCommonPairs.keySet()) {
			this.lessCommonPairs.add(pair);
		}

	}

	private Histogram<Tag> buildCommonPairs() {
		Histogram<Tag> otherTagsHist = new Histogram<>();
		for (Taggable taggable : allTags) {
			Tags tags = taggable.getTags();
			if (tags == null || tags.getCount() == 0) {
				continue;
			}
			for (Tag p : tags.getOtherTags()) {
				otherTagsHist.add(p);
			}
		}
		return otherTagsHist;
	}

	public List<Tag> getLessCommonPairs() {
		return lessCommonPairs;
	}
}
