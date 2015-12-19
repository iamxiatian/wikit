package ruc.irm.wikit.data.dump.parse;

import ruc.irm.wikit.common.conf.Conf;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;


public class WikiPageReader implements Closeable {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    /** processing #index wiki indicator */
    private int index = 0;

    private InputStream stream = null;
    private XMLStreamReader reader = null;

    private int topN = Integer.MAX_VALUE;
    private Conf conf = null;

    public WikiPageReader(Conf conf, InputStream inputStream) throws IOException {
        this(conf, inputStream, Integer.MAX_VALUE);
    }


    public WikiPageReader(Conf conf, InputStream inputStream, int listTopN) throws IOException {
        this.conf = conf;
        this.stream = inputStream;
        this.index = 0;
        this.topN = listTopN;
        try {
            this.reader = XML_INPUT_FACTORY.createXMLStreamReader(inputStream, "UTF-8");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private void handleRevisionElement(WikiPage page) throws XMLStreamException {
        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLEvent.END_ELEMENT && reader.getName().getLocalPart().equals("revision")) {
                return;
            } else if (eventType == XMLEvent.START_ELEMENT) {
                String tag = reader.getName().getLocalPart();
                if (tag.equals("text")) {
                    page.setText(reader.getElementText());
                } else if (tag.equals("ip")) {
                    //System.out.println("ip==>" + reader.getElementText());
                } else if (tag.equals("format")) {
                    page.setFormat(reader.getElementText());
                }
            }
        }
    }

    public WikiPage nextWikiPage() throws XMLStreamException {
        WikiPage page = new WikiPage(conf);
        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLEvent.END_ELEMENT && reader.getName().getLocalPart().equals("page")) {
                if("true".equals(conf.getParam("wiki.page.drill", "true"))) {
                    page.drillMoreInfo();
                }
                return page;
            } else if (eventType == XMLEvent.START_ELEMENT) {
                String tag = reader.getName().getLocalPart();
                if (tag.equals("title")) {
                    page.setTitle(reader.getElementText());
                } else if (tag.equals("id")) {
                    page.setId(Integer.parseInt(reader.getElementText()));
                } else if (tag.equals("ns")) {
                    page.setNs(reader.getElementText());
                } else if ("revision".equals(tag)) {
                     handleRevisionElement(page);
                } else if ("redirect".equals(tag)) {
                    page.setRedirect(reader.getAttributeValue("", "title"));
                } else if ("restrictions".equals(tag)) {

                } else {
                    System.out.println("tag==>" + tag);
                }
            }
        }

        System.err.println("It's strange to return null for nextWikiPage...");
        return null;
    }

    public boolean hasMoreWikiPage() throws XMLStreamException {
        if (index > topN) {
            return false;
        }

        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLEvent.START_ELEMENT:
                    String tag = reader.getName().getLocalPart();
                    if (tag.equals("page")) {
                        index++;
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        stream.close();
    }
}