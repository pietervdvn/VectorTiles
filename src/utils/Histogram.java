package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Histogram<T> {

	private final Map<T, Integer> counts = new HashMap<>();

	public void add(T key) {
		counts.put(key, counts.getOrDefault(key, 0) + 1);
	}

	public Map<T, Integer> getCounts() {
		return counts;
	}

	public int cleanSingletons() {
		int singletons = 0;
		for (Iterator<T> iterator = counts.keySet().iterator(); iterator.hasNext();) {
			T key = iterator.next();
			if (counts.get(key) <= 1) {
				iterator.remove();
				singletons++;
			}
		}
		return singletons;
	}
	
	public Set<T> getSingletons() {
		Set<T> singletons = new HashSet<>();
		for (Iterator<T> iterator = counts.keySet().iterator(); iterator.hasNext();) {
			T key = iterator.next();
			if (counts.get(key) == 1) {
				singletons.add(key);
			}
		}
		return singletons;
	}

}
