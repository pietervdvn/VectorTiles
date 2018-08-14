package vectortile.serialization;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

public abstract class Decoder<T> {
	
	public abstract T deserialize(InputStream out) throws IOException, XMLStreamException;
	
	public T deserialize(String path) throws FileNotFoundException, IOException, XMLStreamException {
		try(InputStream in = new BufferedInputStream(new FileInputStream(new File(path)))){
			return deserialize(in);
		}
	}

}
