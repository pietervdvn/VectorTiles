package swingRender;

import java.awt.Color;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Tags;

public class Styler {

	public static class StylingProperties {

		public Color lineColor, fillColor;

		public StylingProperties(Color lineColor, Color fillColor) {

			this.lineColor = lineColor;
			this.fillColor = fillColor;
		}

	}

	private final TagDecoder global, local;

	private static final Color invis = new Color(128, 0, 0, 0);
	public static final StylingProperties INVISIBLE = new StylingProperties(invis, invis);
	public static final StylingProperties BLACK_WAY = new StylingProperties(Color.BLACK, invis);

	public static final StylingProperties BLACK = new StylingProperties(Color.BLACK, Color.BLACK);
	public static final StylingProperties GRAY = new StylingProperties(Color.LIGHT_GRAY, Color.LIGHT_GRAY);
	public static final StylingProperties GREEN = new StylingProperties(Color.GREEN, Color.GREEN);

	public static final StylingProperties BUILDING = new StylingProperties(Color.ORANGE, Color.ORANGE.darker());

	public Styler(TagDecoder global, TagDecoder local) {
		this.global = global;
		this.local = local;
	}

	public StylingProperties getStylingFor(Types t, Tags tags) {
		if (tags.contains(t, global, local, "leisure", "garden")) {
			return GREEN;
		}
		if (tags.contains(t, global, local, "highway", "residential")) {	
			return BLACK_WAY;
		}
		
		if (tags.contains(t, global, local, "highway", "tertiary")) {
			return BLACK_WAY;
		}
		if(tags.contains(t, global, local, "building", "yes") || tags.contains(t, global, local, "building", "house")) {
			return BUILDING;
		}
		if(tags.contains(t, global, local, "amenity", "college")) {
			return GRAY;
		}
		return INVISIBLE;
	}

}
