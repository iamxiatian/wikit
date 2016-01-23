package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.cache.ArticleCache;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.data.dump.parse.WikiTextParser;
import ruc.irm.wikit.db.Wikipedia;
import ruc.irm.wikit.model.Page;

import java.util.Map;

/**
 * Example:
 *
 *  http://localhost:9090/wiki/article/100
 *  http://localhost:9090/wiki/article?name=hello
 *
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class WikiArticleHandler extends BaseFreemarkerHandler {
    private ArticleCache articleCache = null;

    public WikiArticleHandler() {
        this.articleCache = new ArticleCacheRedisImpl(conf);
    }

    @Override
    protected Map<String, Object> parseContext(Map<String, Object> root, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        Wikipedia wikipedia = new Wikipedia(conf);

        int id = 0;
        if(urlParams.containsKey("id")) {
            id = NumberUtils.toInt(urlParams.get("id"), 0);
        } else if (session.getParms().containsKey("name")) {
            String name = session.getParms().get("name");
            System.out.println("input name is " + name);
            id = articleCache.getIdByNameOrAlias(name);
        }

        root.put("id", id);
        Page page = wikipedia.getPageById(id);
        if (page != null) {
            root.put("title", page.getTitle());
            root.put("type", page.getType());
            root.put("content", page.getContent());
            root.put("links",  WikiTextParser.parseInternalLinks(page
                    .getContent()));
        }
        wikipedia.close();
        return root;
    }

    @Override
    protected String getTemplateName() {
        return "article.ftl";
    }
}
