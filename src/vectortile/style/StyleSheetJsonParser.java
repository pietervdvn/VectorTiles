package vectortile.style;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import vectortile.data.Tag;
import vectortile.data.Tags;

public class StyleSheetJsonParser {

	public static StyleSheet parseStyleSheet(String contents) {

		JSONObject obj = new JSONObject(contents);

		StyleSheet sheet = new StyleSheet(//
				obj.getString("name"), //
				obj.getString("description"), //
				getIntOrNull(obj, "minzoom"), //
				getIntOrNull(obj, "maxzoom")//
		);

		for (Object o : obj.getJSONArray("first-selection")) {
			sheet.addPossiblePrelimaryTags(getCrudeSelection((JSONObject) o));
		}

		JSONObject styles = obj.getJSONObject("styles");
		Set<String> toConsider = new HashSet<>(styles.keySet());
		while (!toConsider.isEmpty()) {
			int sizeBefore = toConsider.size();
			for (Iterator<String> iterator = toConsider.iterator(); iterator.hasNext();) {
				String key = iterator.next();
				JSONObject style = styles.getJSONObject(key);
				if (style.has("base") && toConsider.contains(style.getString("base"))) {
					// Postpone processing this style until the style is resolved
					continue;
				}
				StylingProperties sp = createStyle(sheet, style);
				sheet.addStyleIndex(key, sp);
				iterator.remove();
			}
			if (sizeBefore == toConsider.size()) {
				throw new IllegalStateException("Circular dependency in the style, cannot resolve " + toConsider);
			}
		}
		Condition cond = parseCondition(obj.getJSONObject("styling"), sheet);
		sheet.setCondition(cond);

		return sheet;
	}

	private static Condition parseCondition(JSONObject json, StyleSheet sheet) {

		List<Tag> conditions = new ArrayList<>();
		List<Condition> matchingValues = new ArrayList<>();

		for (String key : json.keySet()) {

			JSONObject values = json.getJSONObject(key);

			for (String value : values.keySet()) {
				Tag t = new Tag(key, value.equals("*") ? null : value);
				conditions.add(t);

				Object style = values.get(value);
				if (style instanceof String) {
					String name = (String) style;
					matchingValues.add(new Condition(sheet.getStyle(name)));
				} else {
					matchingValues.add(parseCondition((JSONObject) style, sheet));
				}
			}
		}
		return new Condition(conditions, matchingValues);
	}

	private static List<Tags> getCrudeSelection(JSONObject tag) {
		List<Tags> currentTags = new ArrayList<>();
		currentTags.add(new Tags(new ArrayList<>()));
		for (String key : tag.keySet()) {
			Object o = tag.get(key);
			if (o instanceof String) {

				String value = tag.getString(key);
				if (value.equals("*")) {
					value = null;
				}
				addToAll(currentTags, new Tag(key, value));
			}

			if (o instanceof JSONArray) {

				JSONArray vals = (JSONArray) o;
				List<Tag> newTags = new ArrayList<>();
				for (Object v : vals) {
					newTags.add(new Tag(key, (String) v));
				}
				currentTags = addToAll(currentTags, newTags);
			}
		}
		return currentTags;
	}

	private static List<Tags> addToAll(List<Tags> origTags, List<Tag> newTags) {
		List<Tags> tagList = new ArrayList<>();
		for (Tags tags : origTags) {
			for (Tag tag : newTags) {
				tagList.add(tags.addOther(tag));
			}
		}
		return tagList;
	}

	private static List<Tags> addToAll(List<Tags> tagList, Tag tag) {
		for (Tags tags : tagList) {
			tags.getOtherTags().add(tag);
		}
		return tagList;
	}

	private static StylingProperties createStyle(StyleSheet sheet, JSONObject properties) {
		StylingProperties sp = new StylingProperties();

		Set<String> unused = new HashSet<>(properties.keySet());
		StylingProperties base = null;
		if (properties.has("base")) {
			String baseName = properties.getString("base");
			base = sheet.getStyle(baseName);
			unused.remove("base");
		}

		for (Field f : sp.getClass().getFields()) {
			if (!properties.has(f.getName())) {
				continue;
			}
			unused.remove(f.getName());
			Object o = properties.get(f.getName());
			try {

				if (f.getType().equals(Color.class)) {
					o = asColor((String) o);
				}
				f.set(sp, o);
			} catch (Exception e) {
				System.err.println("Could not assign " + f.getName() + ": " + e.getMessage());
			}
		}

		if (!unused.isEmpty()) {
			System.err.println("Warning: unused properties in style: " + unused);
		}

		return sp.mergeWith(base);
	}

	private static Color asColor(String color) {
		if (color == null) {
			return null;
		}
		return Color.decode(color);

	}

	private static Integer getIntOrNull(JSONObject o, String key) {
		if (!o.has(key)) {
			return null;
		}
		return o.getInt(key);
	}

}
