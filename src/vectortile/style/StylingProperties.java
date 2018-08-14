package vectortile.style;

import java.awt.Color;
import java.lang.reflect.Field;

public class StylingProperties {

	public Color linecolor, fillcolor;
	public int nodesize = 3
			, linewidth = 1;
	
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

}
