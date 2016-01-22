package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.web.RouterNanoHTTPD;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:30 PM
 */
public abstract class BaseFreemarkerHandler extends RouterNanoHTTPD.DefaultHandler {
    public static Conf conf = ConfFactory.createConf("expt/conf/conf-chinese.xml", true);;

    @Override
    public String getText() {
        return "not implemented";
    }

    public abstract String getText(Map<String, String> urlParams, NanoHTTPD
            .IHTTPSession session);

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
