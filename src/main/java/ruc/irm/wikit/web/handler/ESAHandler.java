package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;
import ruc.irm.wikit.esa.concept.ConceptCache;
import ruc.irm.wikit.esa.concept.ConceptCacheRedisImpl;
import ruc.irm.wikit.esa.concept.vector.ConceptIterator;
import ruc.irm.wikit.esa.concept.vector.ConceptVector;
import ruc.irm.wikit.espm.SemanticPath;
import ruc.irm.wikit.espm.SemanticPathMining;
import ruc.irm.wikit.espm.impl.SemanticPathMiningWikiImpl;
import ruc.irm.wikit.util.ExtendMap;
import ruc.irm.wikit.web.WebContex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class ESAHandler extends BaseFreemarkerHandler {
    private ESAModel esaModel = null;
    private SemanticPathMining espmModel = null;
    private ConceptCache conceptCache = null;

    public ESAHandler() throws WikitException {
        this.esaModel = new ESAModelImpl(WebContex.getInstance().getConf());
        this.espmModel = new SemanticPathMiningWikiImpl(WebContex.getInstance().getConf());
        this.conceptCache = new ConceptCacheRedisImpl(WebContex.getInstance().getConf());
    }

    @Override
    protected Map<String, Object> parseContext(Map<String, Object> root, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        Map<String, String> params = decodeParameters(session);
        String text = params.get("t");
        root.put("t", text);
        System.out.println("input t is " + text);

        if (text != null) {
            try {
                ConceptVector cv = esaModel.getCombinedVector(text, 100);
                if (cv == null) {
                    root.put("msg", "No results.");
                } else {
                    ConceptIterator it = cv.orderedIterator();
                    List<Map<String, Object>> list = new ArrayList<>();
                    while (it.next()) {
                        int cptId = it.getId();
                        double value = it.getValue();
                        String name = conceptCache.getNameById(cptId);
                        String outId = conceptCache.getOutIdById(cptId);
                        Map<String, Object> record = ExtendMap.newMap().append
                                ("name", name)
                                .append("id", cptId)
                                .append("value", value)
                                .append("outId", outId);
                        list.add(record);
                    }
                    root.put("concepts", list);

                    List<SemanticPath> paths = espmModel.getSemanticPaths(text, 50, 10);
                    root.put("paths", paths);
                }
            } catch (WikitException e) {
                root.put("msg", e.toString());
            }
        }
        return root;
    }

    @Override
    protected String getTemplateName() {
        return "esa.ftl";
    }

}
