package vectortile.serialization;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import vectortile.Types;
import vectortile.data.Member;
import vectortile.data.Node;
import vectortile.data.Relation;
import vectortile.data.Tag;
import vectortile.data.Taggable;
import vectortile.data.Tags;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.tools.MultiRemapper;

/**
 * Parses a single piece of XML into a Vectortile
 * 
 * @author pietervdvn
 *
 */
public class OSMXMLParser {

	private static final List<Integer> EMPTY = new ArrayList<>();

	// Lat & lon (times 1000) of the upper left corner of the reference point
	private double minLat, maxLat, minLon, maxLon;

	private final Map<Long, Node> nodes = new HashMap<>();
	private final Map<Long, Way> ways = new HashMap<>();
	private final Map<Long, Relation> relations = new HashMap<>();

	//// RUNNING VARIABLES BELOW

	private Taggable createdLast;

	// The list of tags, which is built while parsing the XML stream and cleared at
	// the end of a node
	private final List<Tag> runningTagList = new ArrayList<>();

	// The list of nodes in a way
	private final List<Long> runningNodeList = new ArrayList<>();
	// The running list of members of a relation
	private final List<Member> runningMemberList = new ArrayList<>();

	// IDentifier of either the relation or way that is being parsed
	private long lastWayID;
	
	
	private final InputStream in;
	
	public OSMXMLParser(InputStream in) {
		this.in = in;
	}
	
	public OSMXMLParser(String path) throws FileNotFoundException {
		this.in = new BufferedInputStream(new FileInputStream(new File(path)));
	}

	public VectorTile deserialize() throws XMLStreamException {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(in);
		while (xmlEventReader.hasNext()) {
			XMLEvent xmlEvent = xmlEventReader.nextEvent();
			if (xmlEvent.isStartElement()) {
				StartElement startElement = xmlEvent.asStartElement();
				handleXMLStartEvent(startElement);

			} else if (xmlEvent.isEndElement()) {
				EndElement endElement = xmlEvent.asEndElement();
				handleXMLCloseEvent(endElement);
			}
		}

		List<Node> nodesList = new ArrayList<Node>();
		List<Way> waysList = new ArrayList<Way>();
		List<Relation> relationsList = new ArrayList<Relation>();

		MultiRemapper mr = new MultiRemapper(//
				buildList(nodes, nodesList), //
				buildList(ways, waysList), //
				buildList(relations, relationsList));
		mr.apply(waysList, relationsList);

		return new VectorTile(minLat, minLon, maxLat, maxLon, nodesList, waysList, relationsList);
	}

	private static <T> Map<Long, Long> buildList(Map<Long, T> data, List<T> addTo) {
		Map<Long, Long> idRemapping = new HashMap<Long, Long>();
		for (Long key : data.keySet()) {
			idRemapping.put(key, (long) addTo.size());
			addTo.add(data.get(key));
		}
		return idRemapping;
	}

	private void handleXMLStartEvent(StartElement elem) {
		switch (elem.getName().getLocalPart()) {
		case "bounds":
			minLat = getAttrDouble(elem, "minlat");
			minLon = getAttrDouble(elem, "minlon");
			maxLat = getAttrDouble(elem, "maxlat");
			maxLon = getAttrDouble(elem, "maxlon");
			break;
		case "node":
			int nLat = getLatLonRel(elem, "lat", minLat);
			int nLon = getLatLonRel(elem, "lon", minLon);
			long id = getAttrLong(elem, "id");

			Node n = new Node(nLat, nLon);
			createdLast = n;
			nodes.put(id, n);
			break;
		case "way":
		case "relation":
			lastWayID = getAttrLong(elem, "id");
			break;

		case "tag":
			runningTagList.add(new Tag(getAttr(elem, "k"), getAttr(elem, "v")));
			break;
		case "nd":
			runningNodeList.add(getAttrLong(elem, "ref"));
			break;
		case "member":
			runningMemberList.add(new Member(Types.valueOf(getAttr(elem, "type").toUpperCase()), //
					getAttrLong(elem, "ref"), //
					getAttr(elem, "role")));
			break;
		}
	}

	private void handleXMLCloseEvent(EndElement elem) {
		switch (elem.getName().getLocalPart()) {
		case "node":
			createdLast.setTags(createTags(Types.NODE));
			break;
		case "way":
			Way w = new Way(createTags(Types.WAY), runningNodeList);
			runningNodeList.clear();
			ways.put(lastWayID, w);
			break;
		case "relation":
			Relation r = new Relation(runningMemberList);
			runningMemberList.clear();
			r.setTags(createTags(Types.RELATION));
			relations.put(lastWayID, r);
			break;
		default:
			break;
		}
	}

	private Tags createTags(Types type) {
		Tags t = new Tags(EMPTY, EMPTY, new ArrayList<>(runningTagList));
		runningTagList.clear();
		return t;
	}

	private static int getLatLonRel(StartElement element, String key, double refLatLon) {
		double parsed = getAttrDouble(element, key);
		double diff = (parsed - refLatLon) * Node.NODE_SCALING_FACTOR;
		return (int) diff;
	}

	private static String getAttr(StartElement element, String name) {
		Attribute attr = element.getAttributeByName(new QName(name));
		if (attr == null) {
			throw new NullPointerException("Missing attribute " + name);
		}
		return attr.getValue();
	}

	private static long getAttrLong(StartElement element, String name) {
		return Long.parseLong(getAttr(element, name));
	}

	private static double getAttrDouble(StartElement element, String name) {
		return Double.parseDouble(getAttr(element, name));
	}

}
