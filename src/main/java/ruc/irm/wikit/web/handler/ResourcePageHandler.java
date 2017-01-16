package ruc.irm.wikit.web.handler;

import fi.iki.elonen.NanoHTTPD;
import ruc.irm.wikit.web.RouterNanoHTTPD;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Page Handler for relatedness calculation
 *
 * @author Tian Xia
 * @date Jan 22, 2016 11:40 PM
 */
public class ResourcePageHandler extends RouterNanoHTTPD.DefaultHandler {

    public ResourcePageHandler() {

    }

    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        String path = uriResource.initParameter(String.class);

        try {
            return NanoHTTPD.newChunkedResponse(getStatus(),
                    NanoHTTPD.getMimeTypeForFile(path),
                    fileToInputStream(path));
        } catch (IOException ioe) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.REQUEST_TIMEOUT, "text/plain", null);
        }
    }


    protected BufferedInputStream fileToInputStream(String resourcePath)
            throws IOException {
        return new BufferedInputStream(this.getClass().getResourceAsStream(resourcePath));
    }

    @Override
    public String getText() {
        throw new IllegalStateException("this method should not be called");
    }

    @Override
    public String getMimeType() {
        throw new IllegalStateException("this method should not be called");
    }

    @Override
    public NanoHTTPD.Response.IStatus getStatus() {
        return NanoHTTPD.Response.Status.OK;
    }
}
