package vectortile.data;

/**
 * Represents a key-value pair
 */
public class Tag {
	public final String key, value;

	public Tag(String key, String value) {
		key.intern();
		value.intern();
		this.key = key;
		this.value = value;
	}
	
	@Override
	public int hashCode() {
		return this.key.hashCode() * this.value.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Tag)) {
			return false;
		}
		Tag p = (Tag) obj;
		return p.key.equals(key)&& p.value.equals(value);
	}
	
	@Override
	public String toString() {
		return key+"="+value;
	}
	
	

}
