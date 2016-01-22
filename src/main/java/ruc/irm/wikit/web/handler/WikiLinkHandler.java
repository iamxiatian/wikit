package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class WikiLinkHandler extends BaseFreemarkerHandler {
    private LinkCache linkCache = null;
    private ArticleCache articleCache = null;

    public WikiLinkHandler() {
        this.linkCache = new LinkCacheRedisImpl(conf);
        this.articleCache = new ArticleCacheRedisImpl(conf);
    }

    @Override
    public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        try {
            Map<String, Object> root = new HashMap<>();

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

            Template template = getCfg().getTemplate("link.ftl");
            StringWriter writer = new StringWriter();
            template.process(root, writer);
            return writer.toString();
        } catch (IOException e) {
            return e.toString();
        } catch (TemplateException e) {
            return e.toString();
        }

//        StringBuilder sb = new StringBuilder();
//
//        int pageId = NumberUtils.toInt(urlParams.get("id"), 0);
//        TIntSet inlinks = linkCache.getInlinks(pageId);
//        TIntSet outlinks = linkCache.getOutlinks(pageId);
//
//        sb.append("id:\t").append(pageId);
//        sb.append("\ninlinks:[").append(inlinks.size()).append("]");
//        inlinks.forEach(new TIntProcedure() {
//            @Override
//            public boolean execute(int id) {
//                try {
//                    sb.append("\n\t").append(id).append("\t");
//                    sb.append(articleCache.getNameById(id));
//                } catch (MissedException e1) {
//                    e1.printStackTrace();
//                }
//                return true;
//            }
//        });
//
//        sb.append("\noutlinks:[").append(outlinks.size()).append("]\n");
//        outlinks.forEach(new TIntProcedure() {
//            @Override
//            public boolean execute(int id) {
//                try {
//                    sb.append("\n\t").append(id).append("\t");
//                    sb.append(articleCache.getNameById(id));
//                } catch (MissedException e1) {
//                    e1.printStackTrace();
//                }
//                return true;
//            }
//        });
//
//        return sb.toString();
    }
}
