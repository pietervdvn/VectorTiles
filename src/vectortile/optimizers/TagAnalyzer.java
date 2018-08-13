package vectortile.optimizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.Histogram;
import vectortile.data.Tag;
import vectortile.data.Taggable;
import vectortile.data.Tags;
/**
 * The tag analyzer groups the 'lessCommon' tagging pairs and replaces them in a Tags-element.
 * This TagAnalyzer will take in all the tags of a single group
 */
public class TagAnalyzer {

	private final Iterable<? extends Taggable> allTags;

	private final Map<Tag, Integer> lessCommonPairsMapping = new HashMap<>();
	private final List<Tag>  lessCommonPairs = new ArrayList<>();

	public TagAnalyzer(Iterable<? extends Taggable> allTags) {
		this.allTags = allTags;
		Histogram<Tag> hist = buildCommonPairs();
		Map<Tag, Integer> lessCommonPairs = hist.getCounts();
		
		int i = 0;
		for (Tag pair : lessCommonPairs.keySet()) {
			this.lessCommonPairs.add(pair);
			lessCommonPairsMapping.put(pair, i);
			i++;
		}
		
	}
	
	public Tags recodeTags(Tags t) {
		
		List<Tag> newOthers = new ArrayList<>();
		List<Integer> newLessCommon = new ArrayList<>();
		for (Tag p : t.getOtherTags()) {
			if(lessCommonPairsMapping.containsKey(p)) {
				newLessCommon.add(lessCommonPairsMapping.get(p));
			}else {
				newOthers.add(p);
			}
		}
		return new Tags(t.getCommonTags(), newLessCommon, newOthers);
		
	}
	
	public void recodeTags(Taggable t) {
		t.setTags(recodeTags(t.getTags()));
	}

	
	public void recodeAll(Iterable<? extends Taggable> ts) {
		for (Taggable t : ts) {
			recodeTags(t);
		}
	}
	
	public void recodeAll() {
		recodeAll(this.allTags);
	}
	
	public Map<Tag, Integer> getLessCommonPairsMapping() {
		return lessCommonPairsMapping;
	}
	
	public List<Tag>  getLessCommonPairs() {
		return lessCommonPairs;
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

}
