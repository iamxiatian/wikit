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
public class ResourcePathHandler extends RouterNanoHTTPD.DefaultHandler {

    public ResourcePathHandler() {

    }

    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        String baseUri = uriResource.getUri();
        String realUri = RouterNanoHTTPD.normalizeUri(session.getUri());
        for (int index = 0; index < Math.min(baseUri.length(), realUri.length()); index++) {
            if (baseUri.charAt(index) != realUri.charAt(index)) {
                realUri = RouterNanoHTTPD.normalizeUri(realUri.substring(index));
                break;
            }
        }

        String path = uriResource.initParameter(String.class);

        //System.out.println("realUrl:" + realUri + "Path is " + path);
        path = path + realUri;
        System.out.printf("new path is " + path);
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
