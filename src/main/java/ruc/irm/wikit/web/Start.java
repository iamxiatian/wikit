package ruc.irm.wikit.web;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:12 PM
 */
public class Start extends NanoHTTPD {

    public Start() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browers to http://localhost:8080/ \n");
    }

    public static void main(String[] args) {
        System.out.println("hello world.");
        try {
            new Start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }
}
