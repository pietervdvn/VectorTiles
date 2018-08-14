package vectortile.swingRender;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SimpleWindow extends JFrame{
	
	public SimpleWindow(JPanel main) {
		setSize(1000, 700);
		add(main);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

}
