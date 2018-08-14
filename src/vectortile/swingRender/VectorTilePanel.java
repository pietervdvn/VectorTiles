package vectortile.swingRender;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Node;
import vectortile.data.VectorTile;
import vectortile.data.Way;
import vectortile.style.EncodedCondition;
import vectortile.style.StyleSheet;
import vectortile.style.StylingProperties;

public class VectorTilePanel extends JPanel {

	private final VectorTile vt;

	private final double PIXELS_PER_METER = 6;

	final private static double R_MAJOR = 6378137.0;
	final private static double R_MINOR = 6356752.3142;

	private final double refLat, refLon;
	private final int offsetX, offsetY;

	private final EncodedCondition ecNodes;
	private final EncodedCondition ecWays;
	private final EncodedCondition ecRels;

	public VectorTilePanel(VectorTile vt, TagDecoder globalDecoder, StyleSheet sh) {
		this(vt, globalDecoder, sh, 0, 0);
	}

	public VectorTilePanel(VectorTile vt, TagDecoder globalDecoder, StyleSheet sh, int offsetX, int offsetY) {
		this.vt = vt;

		refLat = vt.getMaxLatWGS84();
		refLon = vt.getMinLonWGS84();

		this.offsetX = offsetX - lonToX(refLon);
		this.offsetY = offsetY - latToY(refLat);
		Dimension size = new Dimension(lonToX(vt.getMaxLonWGS84()), latToY(vt.getMinLatWGS84()));
		this.setPreferredSize(size);

		this.ecNodes = sh.encode(Types.NODE, globalDecoder, vt);
		this.ecWays = sh.encode(Types.WAY, globalDecoder, vt);
		this.ecRels = sh.encode(Types.RELATION, globalDecoder, vt);

	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawContents(g2);
	}

	private void drawContents(Graphics2D g) {
		long start = System.currentTimeMillis();
		g.setColor(Color.GREEN);
		g.fillOval(0, 0, 3, 3);

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

		for (Node n : vt.getNodes()) {
			Color c = Color.RED;
			if (n.getTags() == null || n.getTags().getCount() == 0) {
				c = Color.GRAY;
			}
			int x = getX(n);
			int y = getY(n);
			g.setColor(c);
			g.fillOval(x - 2, y - 2, 4, 4);

		}

		g.setColor(Color.BLACK);
		for (Way w : vt.getWays()) {
			Node last = null;
			boolean lastGhost = false;
			StylingProperties style = ecWays.resolve(w.getTags());
			if (style == null) {
				continue;
			}
			if (style.fillcolor != null) {
				g.setColor(style.fillcolor);
				g.fillPolygon(way2polygon(w));
			}

			for (long nId : w.getNodes()) {
				Node current = vt.getNode((int) nId);
				boolean isGhost = vt.isGhostNode((int) nId);
				if (last != null) {
					if (isGhost && lastGhost) {

					} else {
						if (style.linecolor == null) {
							break;
						}

						g.setColor(style.linecolor);
						g.setStroke(new BasicStroke(style.linewidth));
						g.drawLine(getX(last), getY(last), getX(current), getY(current));
					}

				}
				lastGhost = isGhost;
				last = current;
			}

		}

		long stop = System.currentTimeMillis();
		System.out.println("Render time: " + (stop - start));

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
		return offsetY + (int) (y * PIXELS_PER_METER * -1);
	}

	private int lonToX(double lon) {
		return offsetX + (int) (R_MAJOR * Math.toRadians(lon) * PIXELS_PER_METER); // (int) (R *
																					// PIXELS_PER_KILOMETER *
																					// Math.log(Math.tan(45 + lon /
																					// 2)));
	}
}
