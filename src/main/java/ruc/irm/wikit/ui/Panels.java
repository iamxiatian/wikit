package ruc.irm.wikit.ui;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.data.dump.parse.WikiTextParser;
import ruc.irm.wikit.db.Wikipedia;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.model.Page;
import ruc.irm.wikit.sr.LinkRelatedness;

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
    public static JPanel createLookupPanel(final Wikipedia wikipedia,
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
                    for (String link : WikiTextParser.parseInternalLinks(page.getContent())) {
                        sb.append(link).append("\t");
                    }
                    sb.append("\n\n");
                    sb.append(page.getContent());
                }
                result.setText(sb.toString());
            }
        });

        titleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText();
                int id = articleCache.getIdByNameOrAlias(title);
                idField.setText(Integer.toString(id));
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


    public static JPanel createRelatednessPanel(final LinkRelatedness relatedness,
                                                final ArticleCache articleCache,
                                                final ESAModel esaModel) {
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
        linePanel.add(new JLabel("ID1:"));
        final JTextField idField1 = new JTextField("508609");
        idField1.setColumns(20);
        linePanel.add(idField1);
        final JTextField idField2 = new JTextField("481572");
        idField2.setColumns(20);
        linePanel.add(idField2);

        JButton goButton = new JButton("Calculate");
        linePanel.add(goButton);
        mainPanel.add(linePanel);

        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        northPanel.add(mainPanel);

        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pageId1 = NumberUtils.toInt(idField1.getText(), 0);
                int pageId2 = NumberUtils.toInt(idField2.getText(), 0);
                String title1 = articleCache.getNameById(pageId1, "Missed.");
                String title2 = articleCache.getNameById(pageId2, "Missed.");

                StringBuilder sb = new StringBuilder();
                sb.append("id1:\t").append(pageId1).append("\t\t");
                sb.append(title1);
                sb.append("\nid2:\t").append(pageId2).append("\t\t");
                sb.append(title2);

                double value = relatedness.googleInlink(pageId1, pageId2);
                sb.append("\nGoogle inlink relatedness is ").append(value);
                sb.append("\nCosine outlink relatedness is ")
                        .append(relatedness.cosineOutlink(pageId1, pageId2));
                value = esaModel.getRelatedness(title1, title2);
                sb.append("\nESA relatedness is ").append(value);
                result.setText(sb.toString());
            }
        });

        return fullPanel;
    }


    public static JPanel createESAPanel(final ArticleCache articleCache,
                                         final ESAModel esaModel,
                                        final ConceptCache conceptCache) {
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
        linePanel.add(new JLabel("ID1:"));
        final JTextField textField = new JTextField("人民大学");
        textField.setColumns(50);
        linePanel.add(textField);

        JButton goButton = new JButton("GO");
        linePanel.add(goButton);
        mainPanel.add(linePanel);

        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        northPanel.add(mainPanel);

        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textField.getText();
                StringBuilder sb = new StringBuilder();
                sb.append(text).append("\n----------------------\n");
                try {
                    ConceptVector cv = esaModel.getCombinedVector(text, 50);
                    ConceptIterator it = cv.orderedIterator();
                    sb.append("\tsn\tconceptId\twikiId\tName\tvalue\n");
                    int sn = 1;
                    while (it.next()) {
                        int conceptId = it.getId();
                        double value = it.getValue();
                        String wikiId = conceptCache.getOutIdById(conceptId);
                        String name = conceptCache.getNameById(conceptId);
                        sb.append("\t").append(sn++);
                        sb.append("\t").append(conceptId).append("\t");
                        sb.append(wikiId).append("\t").append(name).append("\t");
                        sb.append(String.format("%.4f", value)).append("\n");
                    }
                } catch (WikitException e1) {
                    sb.append(e.toString());
                }

                result.setText(result.getText());
            }
        });

        return fullPanel;
    }



}
