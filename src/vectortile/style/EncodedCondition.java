package vectortile.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Tag;
import vectortile.data.Tags;

public class EncodedCondition {

	private final List<List<Integer>> oneOfTags;
	private final List<EncodedCondition> matchingConditions;
	private final StylingProperties style;

	private final TagDecoder global, local;
	private final Types type;

	public EncodedCondition(Types t, TagDecoder global, TagDecoder local, Condition c) {
		this.global = global;
		this.local = local;
		this.type = t;
		if (c.getEntranceConditions() == null) {
			this.style = c.getStyling();
			oneOfTags = null;
			matchingConditions = null;
			return;
		}

		oneOfTags = new ArrayList<>();
		matchingConditions = new ArrayList<>();
		style = null;

		for (int i = 0; i < c.getEntranceConditions().size(); i++) {
			Tag tag = c.getEntranceConditions().get(i);
			List<Integer> entranceOptions = new ArrayList<>();
			if (tag.value == null) {
				entranceOptions.addAll(global.tagsWithKey(t, tag.key, true));
				entranceOptions.addAll(local.tagsWithKey(t, tag.key, false));
			} else {
				Integer value = global.encode(t, tag);
				if (value != null) {
					entranceOptions.add(-1 - value);
				} else {
					value = local.encode(t, tag);
					if(value == null) {
						System.err.println("Warning: dropped "+tag+" as it does not appear in this VT for "+t);
					}else {
						entranceOptions.add(value); 
					}
				}
			}
			
			if(entranceOptions.isEmpty()) {
				continue;
			}
			
			EncodedCondition matchingCond = new EncodedCondition(t, global, local, c.getMatchingStyle().get(i));
			oneOfTags.add(entranceOptions);
			matchingConditions.add(matchingCond);
		}
	}

	public StylingProperties resolve(Tags t) {
		if (oneOfTags == null) {
			return style;
		}
		for (int i = 0; i < oneOfTags.size(); i++) {

			List<Integer> options = oneOfTags.get(i);
			for (Integer needed : options) {
				List<Integer> toSearch;
				if (needed < 0) {
					toSearch = t.getCommonTags();
					needed = -needed - 1;
				} else {
					toSearch = t.getLessCommonTags();
				}
				int index = Collections.binarySearch(toSearch, needed);
				if (index >= 0) {
					// We found it! Lets take this branch!
					return matchingConditions.get(i).resolve(t);
				}
			}
		}
		return null;
	}

}
