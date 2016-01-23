package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.web.RouterNanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:30 PM
 */
public abstract class BaseFreemarkerHandler extends RouterNanoHTTPD.DefaultHandler {
    public static Conf conf = ConfFactory.createConf("expt/conf/conf-chinese.xml", true);

    public BaseFreemarkerHandler() {

    }

    private Configuration getCfg(){
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setNumberFormat("");
        cfg.setClassForTemplateLoading(this.getClass(),
                "/web/template");
        return cfg;
    }

    @Override
    public String getText() {
        return "not implemented";
    }

    protected abstract Map<String, Object> parseContext(Map<String, Object> root,
                                             Map<String, String> urlParams,
                                             NanoHTTPD.IHTTPSession session);

    protected abstract String getTemplateName();

    public String getText(Map<String, String> urlParams, NanoHTTPD
            .IHTTPSession session){
        try {
            Map<String, Object> root = new HashMap<>();
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                root.put("page." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : session.getParms().entrySet()) {
                root.put("page." + entry.getKey(), entry.getValue());
            }

            root = parseContext(root, urlParams, session);
            Template template = getCfg().getTemplate(getTemplateName());
            StringWriter writer = new StringWriter();
            template.process(root, writer);
            return writer.toString();
        } catch (IOException e) {
            return e.toString();
        } catch (TemplateException e) {
            return e.toString();
        }
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public NanoHTTPD.Response.IStatus getStatus() {
        return NanoHTTPD.Response.Status.OK;
    }

    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        String text = getText(urlParams, session);
        ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
        int size = text.getBytes().length;
        return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
    }

}
