package vectortile.style;

import java.util.List;

import vectortile.data.Tag;

public class Condition {

	private final List<Tag> entranceConditions;
	private final List<Condition> matchingStyle;
	private final StylingProperties styling;

	/**
	 * Constructor if there are no more conditions anymore
	 */
	public Condition(StylingProperties properties) {
		entranceConditions = null;
		matchingStyle = null;
		styling = properties;
	}

	/**
	 * Constructor for substyles
	 */
	public Condition(List<Tag> entranceConditions, List<Condition> matchingStyles) {
		this.entranceConditions = entranceConditions;
		this.matchingStyle = matchingStyles;
		styling = null;
	}

	public List<Tag> getEntranceConditions() {
		return entranceConditions;
	}

	public List<Condition> getMatchingStyle() {
		return matchingStyle;
	}

	public StylingProperties getStyling() {
		return styling;
	}
	
	@Override
	public String toString() {
		if(entranceConditions == null) {
			return ""+styling;
		}
		
		
		String result = "";
		for (int i = 0; i < entranceConditions.size(); i++) {
			Tag entr = entranceConditions.get(i);
			result += entr.toString()+ " --> \n  ("+matchingStyle.get(i).toString().replaceAll("\n", "\n  ")+")\n";
		}
		return result;
	}

}
