package vectortile.data;

import java.util.List;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.tools.MultiRemapper;

public class Relation extends Taggable {

	private final List<Member> members;

	public Relation(List<Member> members, Tags t) {
		super(t);
		this.members = members;
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

	public List<Member> getMembers() {
		return members;
	}

	public boolean containsRelation() {
		for (Member member : members) {
			if(member.type == Types.RELATION) {
				return true;
			}
		}
		return false;
	}
}
