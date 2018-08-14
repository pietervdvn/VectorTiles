package vectortile.style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MasterSheet {

	private final List<StyleSheet> styles = new ArrayList<>();

	public MasterSheet(String path) throws IOException {

		List<String> style = Files.readAllLines(Paths.get(path, "DefaultStyles.masterstyle"));

		for (String f : style) {
			List<String> conts = Files.readAllLines(Paths.get(path, f+".style.json"));
			styles.add(StyleSheetJsonParser.parseStyleSheet(String.join("\n", conts)));
		}
		
		Collections.reverse(styles);

	}
	
	public List<StyleSheet> getLayers() {
		return styles;
	}

}
