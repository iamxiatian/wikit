package ruc.irm.wikit.esa.concept;

import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.Cache;
import ruc.irm.wikit.cache.NameIdMapping;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.util.ConsoleLoop;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Cache for concept, for concept title key, it was case-insensitive
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date Aug 3, 2015 17:00 PM
 */
public interface ConceptCache extends NameIdMapping, Cache {

    /**
     * 由于缓存把名称统一转为小写保存，所以存在相同名称对应不同id的情况，通过该方法返回全部id
     *
     * @param name
     * @return
     */
    List<Integer> getAllIdsByName(String name);

    public void saveMaxConceptId(int maxId);

    public int getMaxConceptId();

    /**
     * Save the sum the page views of each concept
     */
    public void saveSumOfPageViews(long pv);

    /**
     * get the sum page views of each concept
     */
    public long getSumOfPageViews();


    public void saveAlias(int id, Collection<String> aliasNames);

    /**
     * Get concept's alias names
     */
    public Collection<String> getAliasNames(int id);

    /**
     * Get concept id by alias names, if alias does not exist, return -1
     */
    public int getIdByAlias(String alias, int defaultValue);


    public void incPageView(int id, int incCount);

    public int getPageViewById(int id);

    /**
     * Save categories belongs to concept <code>id</code>
     *
     * @param categories
     */
    public void saveCategories(int id, Collection<String> categories);

    public Collection<String> getCategoriesById(int id);

    /**
     * Save id-outId relations
     */
    public void saveOutId(int id, String outId);

    public String getOutIdById(int id);

    /**
     * Save link relations from <code>fromId</code> to <code>toId</code>
     */
    public void saveLinkRelation(int fromId, int toId);

    public int getInlinkCount(int id);

    public Set<Integer> getInlinkIds(int id);

    public int getOutlinkCount(int id);

    public Set<Integer> getOutlinkIds(int id);


    /**
     * get concept id by name first, if the name does not exist, then get id
     * by alias name, if still do not exist, return defaultValue.
     */
    public default int getIdByNameOrAlias(String nameOrAlias, int defaultValue) {
        int id = getIdByName(nameOrAlias, -1);
        if (id < 0) {
            id = getIdByAlias(nameOrAlias, defaultValue);
        }
        return id;
    }

    public float getIdf(String term, float defaultValue);

    /**
     * Get term vector, if term does not exist, return null
     */
    public DocScore[] getTfIdf(String term) throws IOException;

    public static final class DocScore {
        public int docId;
        public float score;

        DocScore(int docId, float score) {
            this.docId = docId;
            this.score = score;
        }
    }

    public static void main(String[] args) throws ParseException, IOException, MissedException {
        String helpMsg = "usage: ESAModelBuilder -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("vt", false, "view term tf idf info."));
        options.addOption(new Option("vc", false, "view concept info."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        ConceptCache cache = new ConceptCacheRedisImpl(conf);
        CategoryTreeGraph graph = new CategoryTreeGraphRedisImpl(conf);

        if (commandLine.hasOption("vt")) {
            ConsoleLoop.loop(new ConsoleLoop.Handler() {
                @Override
                public void handle(String text) throws IOException {
                    float idf = cache.getIdf(text, 0);
                    DocScore[] scores = cache.getTfIdf(text);
                    System.out.println("idf:\t" + idf);
                    if (scores != null) {
                        for (DocScore ds : scores) {
                            System.out.println("\t\t" + ds.docId + ":" + ds.score);
                        }
                    } else {
                        System.out.println("No DocScore array for this term.");
                    }
                }
            });
        } else if (commandLine.hasOption("vc")){
            ConsoleLoop.loop(new ConsoleLoop.Handler() {
                @Override
                public void handle(String text) throws IOException {
                    System.out.println("===========================");
                    int id = cache.getIdByNameOrAlias(text, 0);
                    if(id==0){
                        System.out.println("concept does not exist: " + text);
                        return;
                    }

                    System.out.println(id + "\t" + text);
                    System.out.println("Alias:");
                    int count = 1;
                    for (String alias : cache.getAliasNames(id)) {
                        System.out.println("\t" + (count++) + "\t" + alias);
                    }
                    Set<Integer> inlinkIds = cache.getInlinkIds(id);
                    System.out.println("Inlinks" + "[" + inlinkIds.size() + "]:" );
                    System.out.println("\t" + inlinkIds);

                    Set<Integer> outlinkIds = cache.getOutlinkIds(id);
                    System.out.println("outlinkIds" + "[" + outlinkIds.size() + "]:" );
                    System.out.println("\t" + outlinkIds);

                    System.out.println("Categories:");
                    if (graph.hasDone()) {
                        System.out.print("\t{");
                        for (String c : cache.getCategoriesById(id)) {
                            int cid = graph.getIdByName(c, -1);
                            System.out.print(cid + ":" + c + ", ");
                        }
                        System.out.println("}");
                    } else {
                        System.out.println("\t" + cache.getCategoriesById(id));
                    }

                    System.out.println("--------------------------");
                }
            });
        }
    }
}
