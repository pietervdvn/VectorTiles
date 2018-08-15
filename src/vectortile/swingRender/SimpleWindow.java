package vectortile.swingRender;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SimpleWindow extends JFrame{
	
	public SimpleWindow() {

	}
	
	public void goLive() {
		setSize(1000, 700);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

}
