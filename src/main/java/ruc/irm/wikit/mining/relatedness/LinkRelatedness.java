package ruc.irm.wikit.mining.relatedness;

import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.util.SimUtils;

import java.io.IOException;

/**
 * @author Tian Xia
 * @date Jan 20, 2016 10:30 PM
 */
public class LinkRelatedness {
    private Conf conf = null;
    private LinkCache linkCache = null;
    private boolean wlmExtended = false;

    public LinkRelatedness(Conf conf) {
        this.conf = conf;
        this.wlmExtended = conf.getBoolean("wlm.extend", false);
        this.linkCache = new LinkCacheRedisImpl(conf);
    }

    public double getRelatedness(int pageId1, int pageId2) {
        return 0.5*cosineOutlink(pageId1, pageId2) + 0.5*googleInlink
            (pageId1, pageId2);
    }


    public double googleInlink(int pageId1, int pageId2) {
        TIntSet inlinks1 = linkCache.getInlinks(pageId1);
        TIntSet inlinks2 = linkCache.getInlinks(pageId2);

        if (inlinks1.isEmpty() || inlinks2.isEmpty()) {
            return 0.0;
        }

        int a = inlinks1.size();
        int b = inlinks2.size();
        TIntSet intersection = new TIntHashSet(inlinks1.toArray());
        intersection.retainAll(inlinks2);
        int ab = intersection.size();

        if (ab == 0) {
            return 0;
        }

        return 1.0 - (
                      (Math.log(Math.max(a, b)) - Math.log(ab))
                      / (Math.log(linkCache.getTotalPages()) - Math.log(Math.min(a, b)))
                      );
    }

    public double cosineOutlink(int pageId1, int pageId2) {
        TIntSet outlinks1 = linkCache.getOutlinks(pageId1);
        TIntSet outlinks2 = linkCache.getOutlinks(pageId2);

        TIntFloatMap v1 = makeOutlinkVector(outlinks1);
        TIntFloatMap v2 = makeOutlinkVector(outlinks2);
        if (v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        return SimUtils.cosineSimilarity(v1, v2);
    }

    private TIntFloatMap makeOutlinkVector(TIntSet links) {
        TIntFloatMap vector = new TIntFloatHashMap();
        for (int wpId : links.toArray()) {
            vector.put(wpId, (float) Math.log(1.0 * linkCache.getTotalPages() / linkCache
                                              .getInlinks(wpId).size()));
        }

        if(wlmExtended) {
            TIntFloatMap vector2 = new TIntFloatHashMap();
            //考虑二级链接
            for (int id1 : links.toArray()) {
                for (int id2 : linkCache.getOutlinks(id1).toArray()) {
                    double tfidf = Math.log(1.0 * linkCache.getTotalPages()
                            / linkCache.getInlinks(id2).size());
                    float w = (float) tfidf * vector.get(id1);

                    float old = 0;
                    if (vector2.containsKey(id2)) {
                        old = vector2.get(id2);
                    }
                    vector2.put(id2, old + w);
                }
            }
            vector.putAll(vector2);
        }
        return vector;
    }


    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: LinkRelatedness -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("id1", true, "first page id"));
        options.addOption(new Option("id2", true, "second page id"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("id1")
            || !commandLine.hasOption("id2")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        LinkRelatedness calculator = new LinkRelatedness(conf);
        int id1 = Integer.parseInt(commandLine.getOptionValue("id1"));
        int id2 = Integer.parseInt(commandLine.getOptionValue("id2"));
        double relatedness = calculator.getRelatedness(id1, id2);
        System.out.println("Relatedness:" + relatedness);
    }
}
