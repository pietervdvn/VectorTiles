package vectortile.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small util 
 */
public class Remapper {
	
	private Map<Long, Long> remapping = new HashMap<>();
	
	public Remapper(Map<Long, Long> remapping) {
		this.remapping = remapping;
	}
	
	public void rewriteAll(List<Long> values) {
		for (int i = 0; i < values.size(); i++) {
			Long value = values.get(i);
			Long newValue = remapping.get(value);
			if(newValue == null) {
				throw new NullPointerException("Error while remapping node IDS: id "+values.get(i)+" not found");
			}
			if(newValue != value) {
				values.set(i, newValue);
			}
		}
	}
	public long remap(long value) {
		return remapping.get(value);
	}
	
	@Override
	public String toString() {
		String result = "{";
		for (long k : remapping.keySet()) {
			result += k+" --> "+remapping.get(k)+", ";
		}
		return result+"}";
	}
}
