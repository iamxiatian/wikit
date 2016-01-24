package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.LinkCache;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.LinkCacheRedisImpl;
import ruc.irm.wikit.common.exception.MissedException;

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

    public RelatednessHandler() {
        this.linkCache = new LinkCacheRedisImpl(conf);
        this.articleCache = new ArticleCacheRedisImpl(conf);
    }

    @Override
    protected Map<String, Object> parseContext(Map<String, Object> root, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        int pageId = NumberUtils.toInt(urlParams.get("id"), 0);

        root.put("id", pageId);
        root.put("name", articleCache.getNameById(pageId, "Not Existed"));
        root.put("alias", articleCache.getAliasNames(pageId));

        TIntSet inlinks = linkCache.getInlinks(pageId);
        TIntSet outlinks = linkCache.getOutlinks(pageId);
        List<Pair<String, Integer>> listIn = new ArrayList<>();
        inlinks.forEach(new TIntProcedure() {
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
        root.put("inlinks", listIn);

        List<Pair<String, Integer>> listOut = new ArrayList<>();
        outlinks.forEach(new TIntProcedure() {
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
        root.put("outlinks", listOut);
        return root;
    }

    @Override
    protected String getTemplateName() {
        return "link.ftl";
    }

}
