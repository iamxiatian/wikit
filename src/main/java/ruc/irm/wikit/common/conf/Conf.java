package ruc.irm.wikit.common.conf;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import org.apache.lucene.util.Version;
import ruc.irm.wikit.common.exception.WikitException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: xiatian
 * Date: 4/16/14
 * Time: 12:25 PM
 */
public class Conf extends Configuration {
    public static final Version LUCENE_VERSION = Version.LUCENE_47;

    static {
        System.out.println("Loading user defined words for segment.");
        try {
            List<String> lines = Resources.readLines(Conf.class.getResource
                    ("/dict/words.txt"), Charsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("#") || line.trim().length()==0) {
                    continue;
                }
                int pos = line.indexOf("\t");
                if(pos>0) {
                    String word = line.substring(0, pos);
                    String info = line.substring(pos + 1);
                    CustomDictionary.add(word, info);
                } else {
                    CustomDictionary.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> params = new HashMap<>();

    public Conf() {
        super();
    }

    public void setParam(String name, String value) {
        this.params.put(name, value);
    }

    public String getParam(String name) {
        return params.get(name);
    }

    public String getParam(String name, String defaultValue) {
        if (params.containsKey(name)) {
            return params.get(name);
        } else {
            return defaultValue;
        }
    }

    public File getWorkDir() {
        File path = new File(get("work.dir"));
        if(!path.exists()){
            path.mkdirs();
        }
        return path;
    }

    public void setWorkDir(String workDir) {
        set("work.dir", workDir);
    }

    public File getDataDir() {
        File path = new File(get("data.dir"));
        if(!path.exists()){
            path.mkdirs();
        }
        return path;
    }

    public String getEsaIndexDir() {
        return get("esa.index.dir");
    }

    public String getEsaModelDir() {
        String modelDir = get("esa.model.dir");
        return modelDir;
    }

    public void setEsaModelDir(String dir) {
        set("esa.model.dir", dir);
    }

    public String getRedisHost() {
        return get("redis.default.host", "127.0.0.1");
    }

    public void setRedisHost(String redisHost) {
        set("redis.default.host", redisHost);
    }

    public int getRedisPort() {
        return getInt("redis.default.port", 6379);
    }

    public int getRedisTimeout() {
        return getInt("redis.timeout", 12000);
    }

    public void setRedisPort(int redisPort) {
        set("redis.default.port", String.valueOf(redisPort));
    }

    public String getRedisPrefix() {
        return get("redis.prefix", "");
    }

    public void setRedisPrefix(String redisPrefix) {
        set("redis.prefix", redisPrefix);
    }

    public String getMongoHost() {
        return get("mongo.host", "127.0.0.1");
    }

    public void setMongoHost(String mongoHost) {
        set("mongo.host", mongoHost);
    }

    public int getMongoPort() {
        return getInt("mongo.port", 27017);
    }

    public void setMongoPort(int mongoPort) {
        set("mongo.port", String.valueOf(mongoPort));
    }

    public String getMongoDbName() {
        return get("mongo.dbname", "wiki");
    }

    public void setMongoDbName(String mongoDbName) {
        set("mongo.dbname", mongoDbName);
    }

    public String getMongoCollectionPrefix() {
        return get("mongo.prefix", "");
    }

    public void setMongoCollectionPrefix(String mongoCollectionPrefix) {
        set("mongo.prefix", mongoCollectionPrefix);
    }


    public boolean isBig5ToGb() {
        return getBoolean("wiki.big5.to.gb", false);
    }

    public void setBig5ToGb(boolean big5ToGb) {
        set("wiki.big5.to.gb", Boolean.toString(big5ToGb));
    }

    public String getWikiRootCategoryName() {
        return get("wiki.article.category.root", "Main topic classifications");
    }

    public void setWikiRootCategoryName(String categoryName) {
        set("wiki.article.category.root", categoryName);
    }

    public String getUserDefinedSegmentWords() {
        return get("segment.customized.words", null);
    }

    public void setUserDefinedSegmentWords(String userDefinedSegmentWords) {
        set("segment.customized.words", userDefinedSegmentWords);
    }

    public String getWikiPageInlinkFile() {
        return getEsaModelDir() + "/page_inlinks.txt";
    }

    public String getWikiPageOutlinkFile() {
        return getEsaModelDir() + "/page_outlinks.txt";
    }

    public String getWikiTermsIdfFile() {
        return getEsaModelDir() + "/terms_idf.txt";
    }

    public String getWikiTfidfFile() {
        return getEsaModelDir() + "/tfidf.dat";
    }

    public String getWikiRedirectWords() {
        return getEsaModelDir() + "/redirect_words.txt";
    }

    public String getWikiDumpFile() {
        return get("wiki.dump.file");
    }

    public void setWikiDumpFile(String wikiDumpFile) {
        set("wiki.dump.file", wikiDumpFile);
    }

    /**
     * Store valid wiki page(such as inlinks must greater than 5) ids in a file, and return this file.
     * @return
     */
    public File getValidWikiPageIdsFile() {
        return new File(getEsaModelDir(), "filtered.pages.txt");
    }

    public String getEsaLanguage()  {
        return get("esa.language", "English");
    }

    public String getOrError(String name) throws WikitException {
        String value = get(name);
        if (value == null) {
            throw new WikitException("Config property " + name + " does not " +
                    "exist!");
        } else {
            return value;
        }
    }

    /**
     * property must specified, otherwise throw runtime exception
     *
     * @param name
     * @return
     */
    public String getOrRuntimeError(String name) {
        String value = get(name);
        if (value == null) {
            throw new RuntimeException("Config property " + name + " does not specified!");
        } else {
            return value;
        }
    }
}
