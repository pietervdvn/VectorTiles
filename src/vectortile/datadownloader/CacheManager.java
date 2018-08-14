package vectortile.datadownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import vectortile.data.VectorTile;
import vectortile.optimizers.TagCompactor;
import vectortile.optimizers.VectorTileClipper;
import vectortile.optimizers.VectorTileReshuffler;
import vectortile.serialization.BinaryDecoder;
import vectortile.serialization.BinaryEncoder;
import vectortile.serialization.OSMXMLParser;

public class CacheManager {

	private final String cachingDir;

	public CacheManager(String path) {
		this.cachingDir = path;
	}

	public VectorTile retrieveOrDownload(VectorTileID id, double width) throws IOException, XMLStreamException {
		if (isInCache(id)) {
			return retrieve(id);
		}

		return downloadHard(id, width);
	}

	public VectorTile downloadHard(VectorTileID id, double width) throws IOException, XMLStreamException {
		if (isInCache(id)) {
			purgeCache(id);
		}

		VectorTile vt = new OSMXMLParser().download(//
				OSMXMLParser.OPENSTREETMAP_DATA, //
				id.getLat(), id.getLon(), id.getLat() + width, id.getLon() + width);
		System.out.println("Download coompleted"); // TODO Remove sysout

		vt = new VectorTileClipper(vt).createOptimized();
		vt = new TagCompactor(vt).createOptimized();
		vt = new VectorTileReshuffler(vt).createOptimized();

		BinaryEncoder be = new BinaryEncoder(vt);
		
		Path p = id.getPath(cachingDir+".part");
		Files.createDirectories(p.getParent());
		be.serialize(Files.newOutputStream(p));
		Files.move(p, id.getPath(cachingDir));
		
		return vt;
	}

	public VectorTile retrieve(VectorTileID id) throws IOException {
		if (!isInCache(id)) {
			return null;
		}
		BinaryDecoder bd = new BinaryDecoder();
		return bd.deserialize(storageFrom(id));
	}

	private InputStream storageFrom(VectorTileID id) throws IOException {
		Path path = id.getPath(cachingDir);
		return Files.newInputStream(path);
	}

	public boolean isInCache(VectorTileID id) {
		return Files.exists(id.getPath(cachingDir));
	}

	public void purgeCache(VectorTileID id) {
		try {
			Files.delete(id.getPath(cachingDir));
		} catch (IOException e) {
		}
	}

}
