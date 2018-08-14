package swingRender;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;

import swingRender.Styler.StylingProperties;
import vectortile.TagDecoder;
import vectortile.Types;
import vectortile.data.Node;
import vectortile.data.VectorTile;
import vectortile.data.Way;

public class VectorTilePanel extends JPanel {

	private final VectorTile vt;

	private final int PIXELS_PER_DEGREE = 500000;

	private final double refLat, refLon;
	private final int offsetX, offsetY;

	private final Styler styler;

	public VectorTilePanel(VectorTile vt) throws IOException {
		this.vt = vt;

		refLat = vt.getMaxLatWGS84();
		refLon = vt.getMinLonWGS84();
		offsetX = 100;
		offsetY = 150;
		styler = new Styler(new TagDecoder(), vt.getDecoder());
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
			StylingProperties style = styler.getStylingFor(Types.WAY, w.getTags());
			g.setColor(style.fillColor);
			g.fillPolygon(way2polygon(w));
			for (long nId : w.getNodes()) {
				Node current = vt.getNode((int) nId);
				boolean isGhost = vt.isGhostNode((int) nId);
				if (last != null) {
					if (isGhost && lastGhost) {

					} else {
						g.setColor(Color.BLACK);
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
		return offsetY + (int) ((lat - refLat) * (-1) * PIXELS_PER_DEGREE);
	}

	private int lonToX(double lon) {
		return offsetX + (int) ((lon - refLon) * PIXELS_PER_DEGREE);
	}
}
