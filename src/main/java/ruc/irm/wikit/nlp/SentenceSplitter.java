package ruc.irm.wikit.nlp;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;

import java.io.*;
import java.nio.file.Files;

/**
 * Split sentence by opennlp toolkit
 *
 * @author Tian Xia
 * @date Feb 26, 2016 7:24 PM
 */
public class SentenceSplitter {
    private SentenceModel model = null;
    private SentenceDetectorME detector = null;
    private static SentenceSplitter instance = null;

    private SentenceSplitter() {

    }

    public static SentenceSplitter getInstance() throws IOException {
        if(instance==null) {
            instance = new SentenceSplitter();
            instance.load();
        }
        return instance;
    }

    private void load() throws IOException {
        InputStream modelIn = getClass().getResourceAsStream("/opennlp/en-sent.bin");
        this.model = new SentenceModel(modelIn);
        this.detector = new SentenceDetectorME(model);

        if (modelIn != null) {
            try {
                modelIn.close();
            } catch (IOException e) {
            }
        }
    }

    public String[] split(String text) {
        return detector.sentDetect(text);
    }

    /**
     * Read text file and split its content to sentences.
     * @param textFile
     * @return
     */
    public String[] split(File textFile) throws IOException {
        String text = Joiner.on("\n").join(Files.readAllLines(textFile.toPath()));
        System.out.println(text);
        return split(text);
    }

    public static void main(String[] args) throws IOException, ParseException {
        String helpMsg = "usage: SentenceSplitter -f text_file";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("f", true, "text file to split sentences"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("f")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        SentenceSplitter splitter = SentenceSplitter.getInstance();

        File textFile = new File(commandLine.getOptionValue("f"));
        if (!textFile.exists()) {
            System.out.println(commandLine.getOptionValue("f") + " does not exist.");
            return;
        } else {
            String[] sentences = splitter.split(textFile);
            for (String s : sentences) {
                System.out.println(s);
            }
        }
        
    }
}
