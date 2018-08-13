package vectortile.data;

import vectortile.Types;
import vectortile.tools.MultiRemapper;

public class Member {

	public final Types type;
	public long index;
	public final String role;

	public Member(Types type, long index, String role) {
		this.type = type;
		this.index = index;
		this.role = role;
	}
	
	public void remap(MultiRemapper r) {
		index = r.getRewritor(type).remap(index);
	}
	
}
