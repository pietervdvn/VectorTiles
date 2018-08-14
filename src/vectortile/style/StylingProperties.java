package vectortile.style;

import java.awt.Color;
import java.lang.reflect.Field;

public class StylingProperties {

	public Color linecolor, fillcolor, fontcolor;
	public int nodesize = 3
			, linewidth = 1
			, fontsize = 12;
	
	public String font, text, fonteffect;
	
	@Override
	public String toString() {
		String result = "";
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				Object value = f.get(this);
				result += " \"" + f.getName() + "\": \"" + (value == null? "null":value.toString()) + "\",";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "{"+result+"}";
	}
	
	
	public StylingProperties mergeWith(StylingProperties other) {
		if(other == null) {
			return this;
		}
		StylingProperties sp = new StylingProperties();
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				f.set(sp, f.get(other));
				f.set(sp, f.get(this));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sp;
	}
	

}
