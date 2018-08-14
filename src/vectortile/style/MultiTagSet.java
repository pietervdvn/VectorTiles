package vectortile.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Tag;
import vectortile.data.Tags;

public class MultiTagSet {

	private final List<Integer> mustContainAll = new ArrayList<>();
	private final List<List<Integer>> mustContainAny = new ArrayList<>();
	private final List<Integer> mustContainAllLessCommon = new ArrayList<>();
	private final List<List<Integer>> mustContainAnyLessCommon = new ArrayList<>();

	public MultiTagSet(TagDecoder global, TagDecoder local, Types t, Tags tags) {
		for (Tag tag : tags.getOtherTags()) {
			if (tag.value == null) {
				// Search all the tagpairs in the decoders which have this key

				mustContainAny.add(global.tagsWithKey(t, tag.key, false));
				mustContainAnyLessCommon.add(local.tagsWithKey(t, tag.key, false));

			} else {
				Integer encoded = global.encode(t, tag);
				if (encoded != null) {
					mustContainAll.add(encoded);
				} else {
					encoded = local.encode(t, tag);
					mustContainAllLessCommon.add(encoded);
				}
			}
		}

		Collections.sort(mustContainAll);
		Collections.sort(mustContainAllLessCommon);

	}

	public boolean isContained(Tags t) {
		boolean containsMusts = containsAll(mustContainAll, t.getCommonTags()) //
				&& containsAll(mustContainAllLessCommon, t.getLessCommonTags());
		if (!containsMusts) {
			return false;
		}

		for (int i = 0; i < mustContainAny.size(); i++) {
			boolean contained = containsAny(mustContainAny.get(i), t.getCommonTags())
					|| containsAny(mustContainAnyLessCommon.get(i), t.getLessCommonTags());
			if (!contained) {
				return false;
			}
		}
		return true;
	}

	public static boolean containsAll(List<Integer> shouldContain, List<Integer> source) {
		int i = 0;
		for (Integer searched : shouldContain) {
			Integer curValue;

			do {
				curValue = source.get(i);
				i++;
			} while (curValue < searched && i < source.size());
			if (searched != curValue) {
				// The current value overshot the searched value -> The value is not there!
				return false;
			}
			// In the other case: searched == curValue or reached end of the list ->
			// continue
		}
		return true;
	}

	public static boolean containsAny(List<Integer> shouldContain, List<Integer> source) {
		if(shouldContain.isEmpty()) {
			return false;
		}
		int i = 0;
		for (Integer knownValue : source) {

			Integer curSearched;
			do {
				curSearched = shouldContain.get(i);
				i++;
			} while (curSearched < knownValue && i < shouldContain.size());

			if (knownValue == curSearched) {
				// The current tag is contained in the 'shouldContain'-list
				// => We found a match and can stop
				return true;
			}
			// In the other case: searched == curValue or reached end of the list ->
			// continue
		}
		return false;
	}

	@Override
	public String toString() {
		return "Must have global: " + mustContainAll//
				+ " Can have global: " + mustContainAny + " Must have local: " + mustContainAllLessCommon
				+ " Can have local: " + mustContainAnyLessCommon;
	}

}
