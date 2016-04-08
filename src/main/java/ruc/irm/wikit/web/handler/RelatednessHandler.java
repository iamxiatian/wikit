package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.mining.relatedness.EspmRelatedness;
import ruc.irm.wikit.mining.relatedness.LinkRelatedness;
import ruc.irm.wikit.web.WebContex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Page Handler for relatedness calculation
 *
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class RelatednessHandler extends BaseFreemarkerHandler {
    private LinkCache linkCache = null;
    private ArticleCache articleCache = null;
    private LinkRelatedness linkRelatedness;
    private EspmRelatedness espmRelatedness;

    public RelatednessHandler() throws WikitException {
        Conf conf = WebContex.getInstance().getConf();
        this.linkCache = new LinkCacheRedisImpl(conf);
        this.articleCache = new ArticleCacheRedisImpl(conf);
        this.linkRelatedness = new LinkRelatedness(conf);
        this.espmRelatedness = new EspmRelatedness(conf);
    }

    @Override
    protected Map<String, Object> parseContext(Map<String, Object> root, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        String name1 = session.getParms().getOrDefault("name1", "法官");
        String name2 = session.getParms().getOrDefault("name2", "法律");
        if (!session.getParms().containsKey("name1")) {
            root.put("msg", "");
            return root;
        }

        root.put("name1", name1);
        root.put("name2", name2);
        int id1 = articleCache.getIdByNameOrAlias(name1);
        int id2 = articleCache.getIdByNameOrAlias(name2);
        root.put("id1", id1);
        root.put("id2", id2);

        String msg = null;
        if(id1==0 && id2==0) {
            msg = "both " + name1 + " and " + name2 + " do not exist!";
        } else if(id1==0) {
            msg = name1 + " does not exist!";
        } else if (id2 == 0) {
            msg = name2 + " does not exist!";
        }

        if (msg == null) {
            double relatedness = linkRelatedness.getRelatedness(id1, id2);
            root.put("relatedness", relatedness);

            root.put("espmRelatedness", espmRelatedness.calculate(name1, name2));

            TIntSet inlinks1 = linkCache.getInlinks(id1);
            TIntSet inlinks2 = linkCache.getInlinks(id2);
            root.put("inlinks1", inlinks1);
            root.put("inlinks2", inlinks2);

            TIntSet intersection = new TIntHashSet(inlinks1.toArray());
            intersection.retainAll(inlinks2);

            List<Pair<String, Integer>> listIn = new ArrayList<>();
            intersection.forEach(new TIntProcedure() {
                @Override
                public boolean execute(int id) {
                    try {
                        listIn.add(new MutablePair<>(articleCache.getNameById(id), id));
                    } catch (MissedException e1) {
                        e1.printStackTrace();
                    }
                    return true;
                }
            });
            root.put("intersectionInlinks", listIn);

            TIntSet outlinks1 = linkCache.getOutlinks(id1);
            TIntSet outlinks2 = linkCache.getOutlinks(id2);
            root.put("outlinks1", outlinks1);
            root.put("outlinks2", outlinks2);

            intersection = new TIntHashSet(outlinks1.toArray());
            intersection.retainAll(outlinks2);

            List<Pair<String, Integer>> listOut = new ArrayList<>();
            intersection.forEach(new TIntProcedure() {
                @Override
                public boolean execute(int id) {
                    try {
                        listOut.add(new MutablePair<>(articleCache.getNameById(id), id));
                    } catch (MissedException e1) {
                        e1.printStackTrace();
                    }
                    return true;
                }
            });
            root.put("intersectionOutlinks", listOut);
        } else {
            root.put("msg", msg);
        }
        return root;
    }

    @Override
    protected String getTemplateName() {
        return "relatedness.ftl";
    }

}
