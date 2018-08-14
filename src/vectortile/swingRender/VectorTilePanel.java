package vectortile.swingRender;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Node;
import vectortile.data.Tags;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.style.EncodedCondition;
import vectortile.style.MasterSheet;
import vectortile.style.StyleSheet;
import vectortile.style.StylingProperties;

public class VectorTilePanel extends JPanel {

	private final VectorTile vt;
	private final TagDecoder global;

	private double pixelPerMeter = 2;

	final private static double R_MAJOR = 6378137.0;
	final private static double R_MINOR = 6356752.3142;

	private final double refLat, refLon;
	private final int offsetX, offsetY;

	private final MasterSheet sheet;

	public VectorTilePanel(VectorTile vt, TagDecoder globalDecoder, MasterSheet sh) {
		this(vt, globalDecoder, sh, 0, 0);
	}

	public VectorTilePanel(VectorTile vt, TagDecoder globalDecoder, MasterSheet sh, int offsetX, int offsetY) {
		this.vt = vt;
		this.global = globalDecoder;

		refLat = vt.getMaxLatWGS84();
		refLon = vt.getMinLonWGS84();

		this.offsetX = offsetX - lonToX(refLon);
		this.offsetY = offsetY - latToY(refLat);
		Dimension size = new Dimension(lonToX(vt.getMaxLonWGS84()), latToY(vt.getMinLatWGS84()));
		this.setPreferredSize(size);

		this.sheet = sh;

	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Set<Text> texts = new HashSet<>();
		for (StyleSheet s : sheet.getLayers()) {
			drawContents(g2, texts, s);
		}
		
		drawTexts(g2, texts);
	}
	
	private static void drawTexts(Graphics2D g, Set<Text> texts) {
		for (Text text : texts) {
			if(text == null) {
				continue;
			}
			StylingProperties sp = text.style;
			int style;
			switch (sp.fonteffect) {
			case "bold":
				style = Font.BOLD;
				break;
			case "italic":
				style = Font.ITALIC;
				break;
			case "strong":
				style = Font.BOLD;
				break;
			default:
				style = Font.PLAIN;
				break;
			}

			Font f = new Font(sp.font, style, sp.fontsize);
			g.setFont(f);
			g.setColor(sp.fontcolor);
			g.drawString(text.text, text.x, text.y);
		}
	}

	private void drawContents(Graphics2D g, Set<Text> texts, StyleSheet styleSheet) {
		EncodedCondition ecWays = styleSheet.encode(Types.WAY, global, vt);
		EncodedCondition ecNodes = styleSheet.encode(Types.NODE, global, vt);

		Set<Tags> missingStyles = new HashSet<>();
		g.setColor(Color.BLACK);
		for (Way w : vt.getWays()) {
			StylingProperties style = ecWays.resolve(w.getTags());
			if (style == null) {
				missingStyles.add(w.getTags());
				continue;
			}
			drawWay(g, texts, style, w);
		}

		for (Node n : vt.getNodes()) {
			StylingProperties style = ecNodes.resolve(n.getTags());
			if (style == null) {
				missingStyles.add(n.getTags());
				continue;
			}
			drawNode(g, texts, style, n);

		}
	}

	private void drawNode(Graphics2D g, Set<Text> texts, StylingProperties style, Node n) {
		int x = getX(n);
		int y = getY(n);
		g.setColor(style.fillcolor);
		int s = style.nodesize;
		g.fillOval(x - (s / 2), y - (s / 2), s, s);
		texts.add(createText(x, y, Types.NODE, style, n.getTags()));
	}

	private void drawWay(Graphics2D g, Set<Text> texts, StylingProperties style, Way w) {

		if (style.fillcolor != null) {
			g.setColor(style.fillcolor);
			g.fillPolygon(way2polygon(w));
		}
		
		int x =	lonToX(w.getCenter().getWGS84Lon(vt));
		int y =	latToY(w.getCenter().getWGS84Lat(vt));
		texts.add(createText(x, y, Types.WAY, style, w.getTags()));


		if (style.linecolor == null) {
			return;
		}

		g.setColor(style.linecolor);
		g.setStroke(new BasicStroke(style.linewidth));

		Node last = null;
		boolean lastGhost = false;
		for (long nId : w.getNodes()) {
			Node current = vt.getNode((int) nId);
			boolean isGhost = vt.isGhostNode((int) nId);
			if (last != null && !(isGhost && lastGhost)) {
				g.drawLine(getX(last), getY(last), getX(current), getY(current));
			}
			lastGhost = isGhost;
			last = current;
		}
	}

	private Text createText(int x, int y, Types type, StylingProperties sp, Tags t) {
		if (sp.text == null) {
			return null;
		}

		String[] toShowParts = sp.text.split("\\$");
		String toShow = toShowParts[0];
		for (int i = 1; i < toShowParts.length; i++) {
			String part = toShowParts[i];
			if (part.charAt(0) != '(') {
				toShow += part;
				continue;
			}
			int closingPar = part.indexOf(")");
			if (closingPar < 0) {
				toShow += part;
				continue;
			}

			String key = part.substring(1, closingPar);
			toShow += t.getValueOf(type, global, vt.getDecoder(), key) + part.substring(closingPar + 1);
		}
		return new Text(x, y, toShow, sp);
	}

	/**
	 * For debugging only
	 */
	@SuppressWarnings("unused")
	private void drawBBox(Graphics2D g) {
		g.setColor(Color.RED);

		int minY = latToY(vt.getMinLatWGS84());
		int minX = lonToX(vt.getMinLonWGS84());
		int maxY = latToY(vt.getMaxLatWGS84());
		int maxX = lonToX(vt.getMaxLonWGS84());
		System.out.println(minX + ", " + minY + "; " + maxX + ", " + maxY);

		g.drawLine(minX, minY, minX, maxY);
		g.drawLine(minX, maxY, maxX, maxY);
		g.drawLine(maxX, maxY, maxX, minY);
		g.drawLine(maxX, minY, minX, minY);
	}

	private Polygon way2polygon(Way w) {
		List<Long> nds = w.getNodes();
		int[] xs = new int[nds.size()];
		int[] ys = new int[nds.size()];

		for (int i = 0; i < nds.size(); i++) {
			xs[i] = getX(vt.getNode((int) (long) nds.get(i)));
			ys[i] = getY(vt.getNode((int) (long) nds.get(i)));
		}

		return new Polygon(xs, ys, nds.size());
	}

	private int getX(Node n) {
		return lonToX(n.getWGS84Lon(vt));
	}

	private int getY(Node n) {
		return latToY(n.getWGS84Lat(vt));
	}

	private int latToY(double lat) {
		// return offsetY + (int) (Math.PI * R * PIXELS_PER_KILOMETER * lat * (-1));
		if (lat > 89.5) {
			lat = 89.5;
		}
		if (lat < -89.5) {
			lat = -89.5;
		}
		double temp = R_MINOR / R_MAJOR;
		double es = 1.0 - (temp * temp);
		double eccent = Math.sqrt(es);
		double phi = Math.toRadians(lat);
		double sinphi = Math.sin(phi);
		double con = eccent * sinphi;
		double com = 0.5 * eccent;
		con = Math.pow(((1.0 - con) / (1.0 + con)), com);
		double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
		double y = 0 - R_MAJOR * Math.log(ts);
		return offsetY + (int) (y * pixelPerMeter * -1);
	}

	private int lonToX(double lon) {
		return offsetX + (int) (R_MAJOR * Math.toRadians(lon) * pixelPerMeter); 
	}

	private static class Text {

		public int x, y;
		public String text;
		public StylingProperties style;

		public Text(int x, int y, String text, StylingProperties style) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.style = style;
		}
	}
	
	public double getPixelPerMeter() {
		return pixelPerMeter;
	}
	
	public void setPixelPerMeter(double pixelPerMeter) {
		if(this.pixelPerMeter == pixelPerMeter) {
			return;
		}
		this.pixelPerMeter = pixelPerMeter;
		this.repaint();
	}

}
