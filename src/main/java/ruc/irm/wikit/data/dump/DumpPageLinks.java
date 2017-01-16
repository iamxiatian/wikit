package ruc.irm.wikit.data.dump;

import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * User: xiatian
 * Date: 3/27/14
 * Time: 2:57 PM
 */
public class DumpPageLinks {
    private Set<String> concepts = null;
    private Conf conf;

    public DumpPageLinks(Conf conf){
        this.conf = conf;
    }

    public void loadConcepts() throws IOException {
        concepts = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(conf.getWikiTermsIdfFile()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            String[] items = StringUtils.split(line, "\t");
            concepts.add(items[0]);
        }
    }

    public void outlinkToInlink() throws IOException {
        Map<String, Set<Integer>> inlinks = new HashMap<>(1300000);
        String inlinkFile = conf.getWikiPageInlinkFile();
        String outlinkFile = conf.getWikiPageOutlinkFile();
        BufferedReader reader = new BufferedReader(new FileReader(outlinkFile));
        String line = null;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            count++;
            System.out.print(count + "\t");
            if (count % 50 == 0) {
                System.out.print("\n");
            }
            if (line.length() > 0) {
                String[] items = StringUtils.split(line, "\t");
                int wikiId = Integer.parseInt(items[0]);
                for (int i = 1; i < items.length; i++) {
                    String concept = items[i];
                    if (!skip(concept)) {       //skip un-important concepts
                        if (concept.length() > 0) {
                            Set<Integer> wikiIds = inlinks.get(concept);
                            if (wikiIds == null) {
                                wikiIds = new HashSet<Integer>();
                            }
                            wikiIds.add(wikiId);
                            inlinks.put(concept, wikiIds);
                        }
                    }
                }
            }
        }
        reader.close();

        count = 0;
        System.out.println("\nWriting...");
        BufferedWriter writer = new BufferedWriter(new FileWriter(inlinkFile));
        for (Map.Entry<String, Set<Integer>> entry : inlinks.entrySet()) {
            count++;
            if (count % 1000 == 0) {
                System.out.print(count + "\t");
            }
            String key = entry.getKey().trim();

            writer.write(key);
            for (Integer wikiId : entry.getValue()) {
                writer.write("\t" + wikiId);
            }
            writer.write("\n");

        }
        writer.close();

        System.out.println("\nFINISH.");
    }


    private boolean skip(String s) {
        if (concepts != null) {
            return !concepts.contains(s);
        }
        return false;
    }

    private class DumpWikiPageOutLinkFilter implements WikiPageFilter {
        private int count = 0;
        private BufferedWriter writer = null;

        public DumpWikiPageOutLinkFilter(String outlinkFile) throws SQLException, ClassNotFoundException, FileNotFoundException, UnsupportedEncodingException {
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outlinkFile), "utf-8"));
        }

        @Override
        public void process(WikiPage wikiPage,  int index) {
            if (wikiPage.isArticle()) {
                count++;
                if (count % 2000 == 0) {
                    System.out.println(count + "\tINFO: " + wikiPage.getId() + "\t" + wikiPage.getTitle());
                }

                Collection<String> links = wikiPage.getInternalLinks();
                if (links.size() > 0) {
                    try {
                        writer.write(wikiPage.getId());
                        for (String link : links) {
                            writer.write("\t" + link);
                        }
                        writer.write("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void close() throws IOException {
            writer.close();
        }
    }


}
