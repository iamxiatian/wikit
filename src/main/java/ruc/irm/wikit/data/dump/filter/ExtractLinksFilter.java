package ruc.irm.wikit.data.dump.filter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageXmlDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.Big5GB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从维基百科的词条中抽取出链接信息和重定向信息，链接信息保存到json格式的文件中，
 * 而重定向信息保存到了文本文件中，使用时通过命令行运行：<br/>
 *
 * ./run.py ExtractLinksFilter -c xxx.xml -lf links.json -rf redirect.txt
 *
 */
public class ExtractLinksFilter implements WikiPageFilter {
    private BufferedWriter linkWriter = null;
    private BufferedWriter redirectWriter = null;
    private Conf conf;

    public ExtractLinksFilter(Conf conf, File linkOutFile, File
            redirectOutFile) throws
            IOException {
        this.conf = conf;
        linkWriter = new BufferedWriter(new FileWriter(linkOutFile));
        redirectWriter = new BufferedWriter(new FileWriter(redirectOutFile));
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isArticle() ) {
            int id = wikiPage.getId();
            String title = wikiPage.getTitle();
            if (conf.isBig5ToGb()) {
                title = Big5GB.toGB(title);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("title", title);

            if(wikiPage.isRedirect()) {
                try {
                    redirectWriter.append(Integer.toString(id))
                            .append("\t")
                            .append(title).append("\t")
                            .append(wikiPage.getRedirect()).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                map.put("links", wikiPage.getInternalLinks());
                String line = JSON.toJSONString(map);

                try {
                    linkWriter.append(line).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() throws IOException {
        linkWriter.close();
        redirectWriter.close();
    }

    public static void main(String[] args) throws ParseException, IOException {
        String helpMsg = "usage: ExtractLinksFilter -c config.xml";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("lf", true, "Link output json file"));
        options.addOption(new Option("rf", true, "redirect output text file"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c") || !commandLine.hasOption("lf")
                ||!commandLine.hasOption("rf")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        File linkFile = new File(commandLine.getOptionValue("lf"));
        File redirectFile = new File(commandLine.getOptionValue("rf"));
        WikiPageDump dump = new PageXmlDump(conf);
        ExtractLinksFilter filter = new ExtractLinksFilter(conf, linkFile, redirectFile);
        dump.traverse(filter);
        System.out.println("I'm DONE!");
    }
}