package ruc.irm.wikit.web;

/**
 * @author Tian Xia
 * @date Jan 22, 2016 11:20 PM
 */
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.util.ServerRunner;
import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.web.handler.*;

public class Start extends RouterNanoHTTPD {

    private static final int PORT = 9090;

    public static class UserHandler extends DefaultHandler {

        @Override
        public String getText() {
            return "not implemented";
        }

        public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            String text = "<html><body>User handler. Method: " + session.getMethod().toString() + "<br>";
            text += "<h1>Uri parameters:</h1>";
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                text += "<div> Param: " + key + "&nbsp;Value: " + value + "</div>";
            }
            text += "<h1>Query parameters:</h1>";
            for (Map.Entry<String, String> entry : session.getParms().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                text += "<div> Query Param: " + key + "&nbsp;Value: " + value + "</div>";
            }
            text += "</body></html>";

            return text;
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public NanoHTTPD.Response.IStatus getStatus() {
            return NanoHTTPD.Response.Status.OK;
        }

        public NanoHTTPD.Response get(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            String text = getText(urlParams, session);
            ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
            int size = text.getBytes().length;
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
        }

    }

    static public class StreamUrl extends DefaultStreamHandler {

        @Override
        public String getMimeType() {
            return "text/plain";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

        @Override
        public InputStream getData() {
            return new ByteArrayInputStream("a stream of data ;-)".getBytes());
        }

    }

    static class StaticPageTestHandler extends StaticPageHandler {

        @Override
        protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
            if ("exception.html".equals(fileOrdirectory.getName())) {
                throw new IOException("trigger something wrong");
            }
            return super.fileToInputStream(fileOrdirectory);
        }
    }

    /**
     * Create the server instance
     */
    public Start() throws IOException {
        super(PORT);
        addMappings();
        System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
    }

    /**
     * Add the routes Every route is an absolute path Parameters starts with ":"
     * Handler class should implement @UriResponder interface If the handler not
     * implement UriResponder interface - toString() is used
     */
    @Override
    public void addMappings() {
        super.addMappings();

        addRoute("/", ResourcePageHandler.class, "/web/index.html");
        addRoute("/index.html", ResourcePageHandler.class, "/web/index.html");
        addRoute("/main.css", ResourcePageHandler.class, "/web/main.css");
        addRoute("/static/(.)+", ResourcePathHandler.class, "/web/static/");

        addRoute("/wiki/article", WikiArticleHandler.class);
        addRoute("/wiki/article/:id", WikiArticleHandler.class);
        addRoute("/wiki/link/:id", WikiLinkHandler.class);

        addRoute("/relatedness", RelatednessHandler.class);
        addRoute("/esa", ESAHandler.class);

        //addRoute("relatedness/index", );

//        addRoute("/user", UserHandler.class);
//        addRoute("/user/:id", UserHandler.class);
//        addRoute("/user/help", GeneralHandler.class);
//        addRoute("/general/:param1/:param2", GeneralHandler.class);
//        addRoute("/photos/:customer_id/:photo_id", null);
//        addRoute("/test", String.class);
//        addRoute("/interface", UriResponder.class); // this will cause an error
//        // when called
//        addRoute("/toBeDeleted", String.class);
//        removeRoute("/toBeDeleted");
//        addRoute("/stream", StreamUrl.class);
//        addRoute("/browse/(.)+", StaticPageTestHandler.class, new File("src/test/resources").getAbsoluteFile());
    }

    /**
     * Main entry point
     *
     * @param args
     */
    public static void main(String[] args) throws ParseException {
        String helpMsg = "usage: ./run Start -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        System.out.println("Init web context ...");
        WebContex.getInstance().setConf(conf);

        ServerRunner.run(Start.class);
    }
}