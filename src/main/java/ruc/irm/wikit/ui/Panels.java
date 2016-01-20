package ruc.irm.wikit.ui;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.data.dump.parse.WikiTextParser;
import ruc.irm.wikit.db.Wikipedia;
import ruc.irm.wikit.model.Page;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panels which contains all the panel used in Start
 *
 * @author Tian Xia
 * @date Jan 20, 2016 11:11 AM
 */
public class Panels {



    /**
     * Create lookup panel to show wikipedia content
     * 
     * @return
     */
    public static JPanel createLookupPanel(final Wikipedia wikipedia) {
        // 声明总的大面板, fullPanel包括一个NorthPanel和一个centerPanel
        final JPanel fullPanel = new JPanel();
        fullPanel.setLayout(new BorderLayout());

        JPanel northPanel = new JPanel();
        fullPanel.add(northPanel, "North");

        // centerPanel包括了一个文本框
        JPanel centerPanel = new JPanel();
        fullPanel.add(centerPanel, "Center");
        centerPanel.setLayout(new BorderLayout());
        final JTextArea result = new JTextArea();
        // result.setFont(new Font("宋体", Font.PLAIN, 16));
        result.setLineWrap(true);
        JScrollPane centerScrollPane = new JScrollPane(result);
        centerPanel.add(centerScrollPane, "Center");

        northPanel.setLayout(new GridLayout(1, 1));
        // northPanel.add(createWordPanel());
        // northPanel.add(createCilinPanel());

        // 以下加入northPanel中的第一个面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 1));


        JPanel linePanel = new JPanel();
        linePanel.add(new JLabel("   ID:"));
        final JTextField idField = new JTextField("4263516");
        idField.setColumns(50);
        linePanel.add(idField);
        JButton idButton = new JButton("Lookup");
        linePanel.add(idButton);
        mainPanel.add(linePanel);

        linePanel = new JPanel();
        linePanel.add(new JLabel("Title:"));
        final JTextField titleField = new JTextField("");
        titleField.setColumns(50);
        linePanel.add(titleField);
        JButton titleButton = new JButton("Lookup");
        linePanel.add(titleButton);
        mainPanel.add(linePanel);

        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        northPanel.add(mainPanel);

        idButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = idField.getText();
                Page page = wikipedia.getPageById(NumberUtils.toInt(id, 0));
                StringBuilder sb = new StringBuilder();
                sb.append("id:\t").append(id).append("\n");
                if (page != null) {
                    sb.append("title:\t").append(page.getTitle()).append("\n");
                    sb.append("type:\t").append(page.getType()).append("\n");
                    sb.append("internal links:\n");
                    for(String link: WikiTextParser.parseInternalLinks(page.getContent())) {
                        sb.append(link).append("\t");
                    }
                    sb.append("\n\n");
                    sb.append(page.getContent());
                }
                result.setText(sb.toString());
            }
        });

        return fullPanel;
    }

    public static JPanel createLinkPanel(final LinkCache linkCache,
                                         final ArticleCache articleCache) {
        // 声明总的大面板, fullPanel包括一个NorthPanel和一个centerPanel
        final JPanel fullPanel = new JPanel();
        fullPanel.setLayout(new BorderLayout());

        JPanel northPanel = new JPanel();
        fullPanel.add(northPanel, "North");

        // centerPanel包括了一个文本框
        JPanel centerPanel = new JPanel();
        fullPanel.add(centerPanel, "Center");
        centerPanel.setLayout(new BorderLayout());
        final JTextArea result = new JTextArea();
        // result.setFont(new Font("宋体", Font.PLAIN, 16));
        result.setLineWrap(true);
        JScrollPane centerScrollPane = new JScrollPane(result);
        centerPanel.add(centerScrollPane, "Center");

        northPanel.setLayout(new GridLayout(1, 1));
        // northPanel.add(createWordPanel());
        // northPanel.add(createCilinPanel());

        // 以下加入northPanel中的第一个面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 1));


        JPanel linePanel = new JPanel();
        linePanel.add(new JLabel("   ID:"));
        final JTextField idField = new JTextField("100");
        idField.setColumns(50);
        linePanel.add(idField);
        JButton idButton = new JButton("Lookup");
        linePanel.add(idButton);
        mainPanel.add(linePanel);

        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        northPanel.add(mainPanel);

        idButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pageId = NumberUtils.toInt(idField.getText(), 0);
                TIntSet inlinks = linkCache.getInlinks(pageId);
                TIntSet outlinks = linkCache.getOutlinks(pageId);

                StringBuilder sb = new StringBuilder();
                sb.append("id:\t").append(pageId);
                sb.append("\ninlinks:[").append(inlinks.size()).append("]");
                inlinks.forEach(new TIntProcedure() {
                    @Override
                    public boolean execute(int id) {
                        try {
                            sb.append("\n\t").append(id).append("\t");
                            sb.append(articleCache.getNameById(id));
                        } catch (MissedException e1) {
                            e1.printStackTrace();
                        }
                        return true;
                    }
                });

                sb.append("\noutlinks:[").append(outlinks.size()).append("]\n");
                outlinks.forEach(new TIntProcedure() {
                    @Override
                    public boolean execute(int id) {
                        try {
                            sb.append("\n\t").append(id).append("\t");
                            sb.append(articleCache.getNameById(id));
                        } catch (MissedException e1) {
                            e1.printStackTrace();
                        }
                        return true;
                    }
                });

                result.setText(sb.toString());
            }
        });

        return fullPanel;
    }

}
