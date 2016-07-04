package ruc.irm.wikit.expt.espm;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.db.MongoClient;
import ruc.irm.wikit.espm.graph.CategoryTreeGraph;
import ruc.irm.wikit.espm.graph.CategoryTreeGraphRedisImpl;
import ruc.irm.wikit.util.CsvReader;
import ruc.irm.wikit.util.PorterStemmer;

import java.io.IOException;
import java.util.*;

/**
 * Generate odb final test dataset by provided file "ttopics.csv".
 *
 * The final test dataset is stored in mongo db with collection name "odb
 * .category.expt.merge". This collection contains the following fields:
 *
 * <ul>
 *     <li>matchedPath: a string denote the matched categories like:
 *     "Regional/Oceania/Australia/Queensland/Boating", all sub part(such as
 *     Regional, Oceania, etc. ) in this path must appeared in wiki tree-like
 *     category graph and odp path.</li>
 *     <li>desc: a string to describe this path, this description text are
 *     comes from the related odp path description.</li>
 *     <li>odpPaths: an array composed by all odp paths that contains the
 *     matchedPath, like ["Regional/Oceania/Australia/Queensland/Recreation_and_Sports/Boating",
 "Regional/Oceania/Australia/Queensland/Recreation_and_Sports/Boating/Canoeing_and_Kayaking"]</li>
 <li>depth: the depth of matchedPath</li>
 * </ul>
 *
 * @author Tian Xia <a href="mailto:xiat@ruc.edu.cn">xiat@ruc.edu.cn</a>
 * @date May 8, 2014 1:11 PM
 */
public class OdpExptData {
    private static final Logger LOG = LoggerFactory.getLogger(OdpExptData.class);

    private static final String COLL_NAME_FIRST = "odb.category.expt.first";
    static final String COLL_NAME_EXPT = "odb.category.expt.merge";

    static final String COLL_NAME_RAW = "odb.category.raw";

    private Conf conf = null;
    private boolean stemming = false;
    private CategoryTreeGraph treeCache = null;
    private MongoCollection<Document> rawColl = null;
    private MongoCollection<Document> firstExptColl = null;
    private MongoCollection<Document> mergedExptColl = null;

    private String odpFilename = null;
    private int MIN_MATCH_COUNT = 3;

    private PorterStemmer stemmer = null;

    public OdpExptData(Conf conf) throws WikitException {
        this.conf = conf;

        this.odpFilename = conf.get("expt.odp.file.ttopics.csv");
        this.treeCache = new CategoryTreeGraphRedisImpl(conf);
        this.firstExptColl = MongoClient.getOriginCollection(conf, COLL_NAME_FIRST);
        this.mergedExptColl = MongoClient.getOriginCollection(conf, COLL_NAME_EXPT);
        this.rawColl = MongoClient.getOriginCollection(conf, COLL_NAME_RAW);

        this.stemming = conf.getBoolean("expt.odp.stemming", true);
        this.stemmer = new PorterStemmer();

        LOG.debug("odb file \t ==> {}", odpFilename);
        LOG.debug("stemming \t ==> {}", Boolean.toString(stemming));
    }

    /**
     * find all the both-occur cateogry in wiki page and odp, and save to Set,
     * all names are converted into lowercase format.
     *
     * @param odpCategories
     * @return
     */
    public Set<String> findCooccuredCategories(Set<String> odpCategories) {
        Set<String> cooccurCategories = new HashSet<>();

        Set<String> wikiCatNames = treeCache.listNames();
        int count = 0;
        int same = 0;
        for (String name : wikiCatNames) {
            count++;

            String wikiTitle = name.toLowerCase();
            if (stemming) {
                wikiTitle = stemmer.stem(wikiTitle);
            }

            if (odpCategories.contains(wikiTitle)) {
                same++;
                cooccurCategories.add(wikiTitle);
                System.out.println(name + " / " + wikiTitle);
            }
        }

        System.out.println(count + ":\t co-occur==>" + same + "/" + odpCategories.size());
        return cooccurCategories;
    }

    private List<String> getMatchedNames(Set<String> cooccuredCategories, String[] odpPathParts) {
        List<String> matchedNames = new ArrayList<>();
        for (String part : odpPathParts) {
            String matchingName = part.toLowerCase();
            if (stemming) {
                matchingName = stemmer.stem(matchingName);
            }
            if (cooccuredCategories.contains(matchingName)) {
                matchedNames.add(part);
            }
        }

        return matchedNames;
    }

    private void saveExperimentODPCategories(Set<String> cooccuredNames) throws IOException {
        System.out.println("Drop odp expt collection...");
        firstExptColl.drop();
        firstExptColl.createIndex(new Document("path", 1));

        CsvReader reader = new CsvReader(odpFilename);

        System.out.println("Scan odp categories...");
        while (reader.readRecord()) {
            String id = reader.get(0);
            String path = reader.get(1);
            String title = reader.get(3);
            String desc = reader.get(4);
            if (StringUtils.isEmpty(desc)) {
                //skip item which has no description
                continue;
            } else if (StringUtils.isNumeric(title)) {
                //skip number category
                continue;
            }

            String comparedTitle = title.toLowerCase();
            if (stemming) {
                comparedTitle = stemmer.stem(comparedTitle);
            }

            String[] pathParts = path.split("/");
            //remove the category named "Top"
            pathParts = Arrays.copyOfRange(pathParts, 1, pathParts.length);
            List<String> matchedNames = getMatchedNames(cooccuredNames, pathParts);

            if (matchedNames.size() >= MIN_MATCH_COUNT) {
                if (firstExptColl.count(new BasicDBObject("path", path)) == 0) {
                    //like Top/Arts/Animation/Anime/Titles/S/Slayers
                    Document item = new Document("id", id)
                            .append("title", title)
                            .append("path", StringUtils.join(pathParts, "/"))
                            .append("matched", matchedNames)
                            .append("desc", desc);
                    //save to mongodb
                    firstExptColl.insertOne(item);

                }
            }
        }
        System.out.println("I'm DONE for save experiment odp categories!");
    }

    /**
     * Merge the records in expt collection with same path, and save to
     * merged collection. This collection will be used in experiment.
     */
    private void mergeSameOdpPaths() {
        MongoCursor<Document> cursor = firstExptColl.find().iterator();
        Map<String, String> descriptionCache = new HashMap<>();
        Map<String, List<String>> odpPathCache = new HashMap<>();

        while (cursor.hasNext()) {
            Document item = cursor.next();
            String odpPath = (String) item.get("path");
            List<String> matchedNames = (List<String>) item.get("matched");
            String description = (String) item.get("desc");
            String matchedPath = StringUtils.join(matchedNames, "/");
            if (descriptionCache.containsKey(matchedPath)) {
                descriptionCache.put(matchedPath, descriptionCache.get(matchedPath) + "\n" + description);
            } else {
                descriptionCache.put(matchedPath, description);
                odpPathCache.put(matchedPath, new ArrayList<String>());
            }
            odpPathCache.get(matchedPath).add(odpPath);
        }

        mergedExptColl.drop();
        for (Map.Entry<String, String> entry : descriptionCache.entrySet()) {
            String matchedPath = entry.getKey();
            String[] matchedPathArray = StringUtils.split(entry.getKey(), "/");
            Document item = new Document("matchedPath", matchedPath)
                    .append("desc", entry.getValue())
                    .append("odpPaths", odpPathCache.get(entry.getKey()))
                    .append("depth", matchedPathArray.length);
            mergedExptColl.insertOne(item);
        }
        System.out.println("Finish merge work!");
    }

    private Set<String> loadODPCategoryTitle() throws IOException {
        CsvReader reader = new CsvReader(odpFilename);
        PorterStemmer stemmer = new PorterStemmer();
        Set<String> odpCategories = new HashSet<>();
        System.out.println("load odp categories from " + odpFilename);
        while (reader.readRecord()) {
            String title = reader.get(3).toLowerCase();
            String description = reader.get(4);
            if (title.length() <= 1) {
                continue;
            }
            if (description.length() > 5) {
                continue;
            }
            if (stemming) {
                title = stemmer.stem(title);
            }

            odpCategories.add(title);
        }

        return odpCategories;
    }

    /**
     * Setup experimental data to mongodb.
     */
    public void setupExptData() {
        try {
            dumpRawODPCategoriesToMongo();
            Set<String> odpCategories = loadODPCategoryTitle();
            Set<String> cooccuredCategories = findCooccuredCategories(odpCategories);
            saveExperimentODPCategories(cooccuredCategories);
            mergeSameOdpPaths();
        } catch (IOException e) {
            LOG.error("setup expt data for ODB error.", e);
        }
    }

    /**
     * Dump all the odp categories into mongodb database
     *
     * @throws IOException
     */
    private void dumpRawODPCategoriesToMongo() throws IOException {
        CsvReader reader = new CsvReader(odpFilename);
        PorterStemmer stemmer = new PorterStemmer();

        while (reader.readRecord()) {
            String id = reader.get(0);
            String path = reader.get(1);
            String title = reader.get(3);
            String desc = reader.get(4);
            String stem = stemmer.stem(title);

            Document item = new Document("id", id)
                    .append("title", title)
                    .append("stem", stem)
                    .append("path", path)
                    .append("desc", desc);
            //save to mongodb
            rawColl.insertOne(item);
        }

        System.out.println("I'm done for dump to mongo.");
    }


    /**
     * Get all odp level 1 category names and occurred count
     */
    public Map<String, Integer> getLevelOneInfo() throws IOException {
        Map<String, Integer> root = new HashMap<>();
        CsvReader reader = new CsvReader(odpFilename);
        PorterStemmer stemmer = new PorterStemmer();

        while (reader.readRecord()) {
            String id = reader.get(0);
            String path = reader.get(1);

            String c =   StringUtils.split(path, "/")[1];
            if (root.containsKey(c)) {
                root.put(c, root.get(c) + 1);
            } else {
                root.put(c, 1);
            }
        }

        return root;
    }

    public static void main(String[] args) throws IOException, WikitException, ParseException {
        String helpMsg = "usage: OdbExptData -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("build", false, "build odb expt data."));
        options.addOption(new Option("topname", false, "view top one names."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        OdpExptData odp = new OdpExptData(conf);
        if (commandLine.hasOption("build")) {
            odp.setupExptData();
        } else if(commandLine.hasOption("topname")){
            Map<String, Integer> root = odp.getLevelOneInfo();
            for (Map.Entry<String, Integer> entry : root.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }
}
