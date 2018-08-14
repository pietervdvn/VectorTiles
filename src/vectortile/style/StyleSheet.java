package vectortile.style;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Tags;
import vectortile.data.VectorTile;

public class StyleSheet {

	private final Map<String, StylingProperties> styles = new HashMap<>();
	private final Set<Tags> requiredTags = new HashSet<>();
	
	private Condition condition;

	@SuppressWarnings("unused")
	private final String name, description;
	@SuppressWarnings("unused")
	private final Integer minzoom, maxzoom;

	public StyleSheet(String name, String description, Integer minzoom, Integer maxzoom) {
		this.name = name;
		this.description = description;
		this.minzoom = minzoom;
		this.maxzoom = maxzoom;
	}

	public void addStyleIndex(String name, StylingProperties style) {
		styles.put(name, style);
	}

	public void addPossiblePrelimaryTags(List<Tags> list) {
		requiredTags.addAll(list);
	}

	@Override
	public String toString() {
		String result = "";
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				Object value = f.get(this);
				result += " \"" + f.getName() + "\": \"" + (value == null ? "null" : value.toString()) + "\",";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public StylingProperties getStyle(String style) {
		if(!hasStyle(style)) {
			throw new IllegalArgumentException("Style "+style+" not found. Make sure it is declared and doublecheck for typos."
					+ "\nKnown styles are "+styles.keySet());
		}
		return styles.get(style);
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public boolean hasStyle(String name) {
		return styles.containsKey(name);
	}

	public EncodedCondition encode(Types t, TagDecoder global, VectorTile vt) {
		return new EncodedCondition(t, global, vt.getDecoder(), condition);
	}
	
	public List<MultiTagSet> getPreliminaryChoicesFor(Types t, VectorTile vt, TagDecoder global){
		List<MultiTagSet> mtss = new ArrayList<>();
		for (Tags req : requiredTags) {
			MultiTagSet mts = new MultiTagSet(global, vt.getDecoder(), t, req);
			mtss.add(mts);
		}
		return mtss;
	}

	public Map<String, StylingProperties> getStyles() {
		return styles;
	}

}
