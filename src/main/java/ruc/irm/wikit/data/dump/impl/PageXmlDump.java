package ruc.irm.wikit.data.dump.impl;

import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.filter.SampleFilter;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageReader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>PageXmlDump parse original xml dumps provided by wikipeida.org, all wiki
 * pages will be accessed when traverse the data set.</p>
 * <p>
 * <p>
 * The wiki xml parser is based on: <a href="http://code.google.com/p/gwtwiki/">gwtwiki</a>
 * </p>
 *
 * @author Tian Xia
 * @date Mar 23, 2014 12:02 AM
 */
public class PageXmlDump extends WikiPageDump {
    private WikiPageReader reader = null;

    public PageXmlDump(Conf conf) {
        this.conf = conf;
        this.dumpFile = conf.getWikiDumpFile();
    }

    @Override
    public void open() throws IOException {
        if (!new File(dumpFile).exists()) {
            throw new IOException("xml dump file does not exist ==> " +
                    dumpFile);
        }

        InputStream stream = new FileInputStream(dumpFile);
        if (dumpFile.endsWith(".bz2")) {
            boolean multiStream = dumpFile.contains("multistream");
            stream = new BZip2CompressorInputStream(stream, multiStream);
        }

        this.reader = new WikiPageReader(conf, stream);
    }

    @Override
    public void close() throws IOException {
        if(reader!=null) reader.close();
    }

    @Override
    public boolean hasNext() {
        try {
            return reader.hasMoreWikiPage();
        } catch (XMLStreamException e) {
            LOG.error("Error occurred when parse wiki xml", e);
            return false;
        }
    }

    @Override
    public WikiPage next() {
        try {
            return reader.nextWikiPage();
        } catch (XMLStreamException e) {
            LOG.error("Error occurred when read next wiki from xml", e);
            return null;
        }
    }

    public static void main(String[] args) throws ParseException, XMLStreamException, IOException {
        String helpMsg = "usage: PageXmlDump -c config.xml --mr\n "
                + "PageXmlDump -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));

        options.addOption(new Option("sample", false, "sample 1% from " +
                "articles"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);

        PageXmlDump dump = new PageXmlDump(conf);
        if (commandLine.hasOption("sample")) {
            SampleFilter sampleFilter = new SampleFilter(conf);
            dump.traverse(sampleFilter);
            System.out.println("I'm DONE for sample!");
        }
    }
}
