package ruc.irm.wikit.ui;

import javax.swing.*;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * About wikit
 *
 * @author Tian Xia
 * @date Jan 20, 2016 11:11 AM
 */
public class About extends JFrame {
  private static final long serialVersionUID = -2307582155443587993L;

	public static JPanel createPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		JTextPane editorPane = new JTextPane();
		editorPane.setEditable(false); 
		//wrap long line
		editorPane.setEditorKit(new StyledEditorKit());
		editorPane.setContentType("text/html");		
		try {
			URLClassLoader urlLoader = (URLClassLoader)About.class.getClassLoader();
			String html = About.class.getPackage().getName().replaceAll("\\.", "/") + "/about.html";
			System.out.println(html);
			URL url = urlLoader.findResource(html);//可以用html格式文件做你的帮助系统了
	    editorPane.setPage(url);
    } catch (IOException e1) {
	    editorPane.setText(e1.getMessage());
    } 
		//editorPane.setText("<html><body>个人主页：<a href='xiatian.irm.cn'>http://xiatian.irm.cn/</a></body></html>");
		
		
		mainPanel.add(new JScrollPane(editorPane), BorderLayout.CENTER);
		return mainPanel;
	}
	
	public About() {
		this.setTitle("About Wikit");
		
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(600, 400));
		this.getContentPane().add(createPanel());
		this.pack();		
	}
	
	public static void main(String[] args) {
	  new About().setVisible(true);
  }
}
