package vectortile.swingRender;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import vectortile.TagDecoder;
import vectortile.datadownloader.CacheManager;
import vectortile.datadownloader.VectorTileID;
import vectortile.style.MasterSheet;

public class LoadingPanel extends JPanel {

	boolean loading = true;

	public LoadingPanel(VectorTileID vid, CacheManager cm, final TagDecoder global, final MasterSheet sheet) {
		
		setPreferredSize(new Dimension(500, 500));

		try {
			URL url = new URL("file:///home/pietervdvn/git/VectorTiles/res/Loading.gif");
			Icon icon = new ImageIcon(url);
			this.add(new JLabel(icon), BorderLayout.CENTER);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(!cm.isInCache(vid)) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
				
				LoadingPanel.this.removeAll();
				try {
					LoadingPanel.this.add(new VectorTilePanel(cm.retrieve(vid), global, sheet));
					LoadingPanel.this.repaint();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}).start();;

	}

}
