package vectortile.style;

import java.lang.reflect.Field;
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

	private final String name, description;
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

}
