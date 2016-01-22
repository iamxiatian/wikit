package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.data.dump.parse.WikiTextParser;
import ruc.irm.wikit.db.Wikipedia;
import ruc.irm.wikit.model.Page;

import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class WikiArticleHandler extends BaseFreemarkerHandler {
    private Wikipedia wikipedia = new Wikipedia(conf);

    @Override
    public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        String id = urlParams.get("id");
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
        return sb.toString();
    }

    @Override
    public String getMimeType() {
        return "text/plain";
    }

}
