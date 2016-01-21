package ruc.irm.wikit.ui;

import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.db.Wikipedia;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.sr.LinkRelatedness;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

/**
 * The main UI for test function of Wikit
 *
 * @author Tian Xia
 * @date Jan 20, 2016 11:11 AM
 */
public class Start extends JFrame {

	private static final long serialVersionUID = 85744461208L;

	private Wikipedia wikipedia = null;
	private LinkCache linkDb = null;
	private ArticleCache articleCache = null;
	private ESAModel esaModel = null;

	public Start(Conf conf) {
		this.setTitle("Wikit--Wikipedia toolkit");
		this.setSize(420, 700);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.wikipedia = new Wikipedia(conf);
		this.linkDb = new LinkCacheRedisImpl(conf);
		this.articleCache = new ArticleCacheRedisImpl(conf);
		this.esaModel = new ESAModelImpl(conf);
		// //////////////////////////////////
		// add menu
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add(new JMenuItem("Exit"));

		JMenu helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);
		helpMenu.add(new JMenuItem("Help"));

		Container contentPane = this.getContentPane();
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("View Wiki Page", Panels.createLookupPanel(wikipedia,
				articleCache));
		tabbedPane.add("View Links", Panels.createLinkPanel(linkDb, articleCache));
		tabbedPane.add("Relatedness calculation", Panels.createRelatednessPanel(
				new LinkRelatedness(conf), articleCache, esaModel));
		tabbedPane.add("About", About.createPanel());
		JScrollPane scrollPane = new JScrollPane(tabbedPane);
		contentPane.add(scrollPane);
		
		this.pack();
		setExtendedState(MAXIMIZED_BOTH);

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Closing");
				wikipedia.close();
			}
		});
	}

	public static void InitGlobalFont(Font font) {
		FontUIResource fontRes = new FontUIResource(font);
		for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource) {
				UIManager.put(key, fontRes);
			}
		}
	}

	public static void main(String[] args) {
		Conf conf = ConfFactory.createConf("expt/conf/conf-chinese.xml", true);
		//JFrame.setDefaultLookAndFeelDecorated(true);
		//解决字体在Ubuntu中显示有乱码的问题
		InitGlobalFont(new Font("Microsoft YaHei", Font.TRUETYPE_FONT, 12));
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				Start w = new Start(conf);
				w.setVisible(true);
			}
		});
	}

}
