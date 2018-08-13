package vectortile.data;

import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.tools.MultiRemapper;

public class Relation extends Taggable {

	public final Member[] members;

	public Relation(List<Member> members) {
		this.members = new Member[members.size()];
		for (int i = 0; i < this.members.length; i++) {
			this.members[i] = members.get(i);
		}
	}

	public void rewrite(MultiRemapper r) {
		for (Member member : members) {
			member.remap(r);
		}
	}

	public String toString(TagDecoder td) {
		String result = "Relation ";
		for (Member member : members) {
			result += member + ", ";
		}
		result += getTags().toString(Types.RELATION, td);

		return result;
	}

}
