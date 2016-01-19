package ruc.irm.wikit.expt;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.compress.utils.IOUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.esa.ESAModel;
import ruc.irm.wikit.esa.ESAModelImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Experiment for relatedness calculation.
 *
 * @author Tian Xia
 * @date Jan 18, 2016 10:31 AM
 */
public class RelatednessExpt {
    private Conf conf;

    private RelatednessExpt(Conf conf) {
        this.conf = conf;
    }

    /**
     * 根据人工给出的输入文件，根据ESA方法计算每一对词语的相关度，并把结果输出到文件中。
     * 输入文件的格式为：
     * <br/>
     * <pre>
     * 词汇1（word 1）	词汇2（word2）	人工评测的平均值
     * 浏览器	网页	8.93
     * </pre>
     * 以上每一项之间通过TAB符号分割
     *
     * @param input 输入文件
     * @param output
     */
    public void calculateByESA(File input, File output) throws IOException {
        ESAModel model = new ESAModelImpl(conf);
        StringBuilder rawValues = new StringBuilder();
        StringBuilder calculatedValues = new StringBuilder();
        BufferedWriter writer = Files.newWriter(output, Charsets.UTF_8);

        List<String> results = new LinkedList<>();
        List<String> lines = Files.readLines(input, Charsets.UTF_8);
        writer.write(lines.get(0) + "\tESA_Results\n");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().length() == 0) {
                continue;
            }
            List<String> parts = Splitter.on("\t").splitToList(line);
            String firstWord = parts.get(0);
            String secondWord = parts.get(1);
            String rawValue = parts.get(2);
            double relatedness = model.getRelatedness(firstWord, secondWord);
            String newValue =  String.format("%.4f", relatedness);
            writer.write(line + "\t" + newValue+"\n");

            if (rawValues.length() == 0) {
                rawValues.append("[").append(rawValue);
                calculatedValues.append("[").append(newValue);
            } else {
                rawValues.append(", ").append(rawValue);
                calculatedValues.append(", ").append(newValue);
            }
        }
        rawValues.append("]");
        calculatedValues.append("]");

        writer.append("import numpy\n");
        writer.append("list1 = ").append(rawValues).append("\n");
        writer.append("list2 = ").append(calculatedValues).append("\n");
        writer.append("numpy.corrcoef(list1, list2)[0, 1]\n");
        writer.close();
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: RelatednessExpt -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("in", true, "input file(word-240.txt)"));
        options.addOption(new Option("out", true, "output file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("in")
                || !commandLine.hasOption("out")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        String in = commandLine.getOptionValue("in");
        String out = commandLine.getOptionValue("out");
        RelatednessExpt expt = new RelatednessExpt(conf);
        expt.calculateByESA(new File(in), new File(out));
        System.out.println("I'm DONE!");
    }

}
