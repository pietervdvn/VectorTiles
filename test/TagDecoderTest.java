import java.io.IOException;

import org.junit.Test;

import vectortile.TagDecoder;
import vectortile.Types;

public class TagDecoderTest {

	@Test
	public void test() throws IOException {
		TagDecoder td = new TagDecoder();
		System.out.println(td.decode(Types.NODE,(byte) 0));
		System.out.println(td.decode(Types.WAY, (byte) 0));
	}

}
