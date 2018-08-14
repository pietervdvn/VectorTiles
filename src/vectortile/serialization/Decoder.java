package vectortile.serialization;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

public abstract class Decoder<T> {

	public abstract T deserialize(InputStream out) throws IOException, XMLStreamException;

	public T deserialize(String path) throws FileNotFoundException, IOException, XMLStreamException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(new File(path)))) {
			return deserialize(in);
		}
	}

	public T download(String location, double minlat, double minlon, double maxlat, double maxlon)
			throws IOException, XMLStreamException {
		location = location.replace("{minlon}", "" + minlon).replace("{minlat}", "" + minlat).replace("{maxlon}", "" + maxlon)
				.replace("{maxlat}", "" + maxlat);
		System.out.println("Fetching from "+location);
		URL url = new URL(location);
		try(InputStream in = url.openStream()){
			return deserialize(in);
		}
	}

}
