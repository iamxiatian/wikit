package ruc.irm.wikit.data.export;

import org.apache.commons.cli.*;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.nlp.segment.SegWord;
import ruc.irm.wikit.nlp.segment.Segment;
import ruc.irm.wikit.nlp.segment.SegmentFactory;
import ruc.irm.wikit.util.ProgressCounter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 把所有的维基百科的标题和正文切分后，合并输出到文本文件中，用于训练word2vec模型。
 * 使用方法：<br/>
 *
 * ./run.py SegArticles2Txt -df wiki.article.dump.gz -o wiki4word2vec.txt
 *
 *  <br/>
 *  进一步过滤空行：
 *  awk '/^[^$]/' wiki4word2vec.txt > wiki4word2vec.no_blank_line.txt
 * @author Tian Xia
 * @date Aug 18, 2016 15:03
 */
public class SegArticles2Txt {
    private Segment segment = null;

    public SegArticles2Txt(Conf conf) {
        this.segment = SegmentFactory.getSegment(conf);
    }

    public void segAndOut(WikiPageDump dump, File outTxtFile) throws IOException {
        dump.open();

        //从维基百科中发现的新词语
        File newWordFile = new File("/home/xiatian/data/wiki/wiki_new_words.dic");
        PrintWriter newWordDictWriter = new PrintWriter(new FileWriter(newWordFile));

        //输出的维基百科文章切分结果
        PrintWriter writer = new PrintWriter(new FileWriter(outTxtFile));
        ProgressCounter counter = new ProgressCounter();
        while (dump.hasNext()) {
            WikiPage page = dump.next();
            counter.increment();

            if(!page.isArticle()) continue;

            String title = page.getTitle();
            if(title.length()<5 && !title.contains("( ")) {
                segment.insertUserDefinedWord(title, "nz", 100);
                newWordDictWriter.println(title);
            }
            String text = page.getPlainText();

            List<SegWord> terms = segment.tag(title + "\n" + text);
            for (SegWord term : terms) {
                String name = term.word;
                String pos = term.pos;
                if(name.equals("\n") || name.equals("\r") || name.equals("。")
                        || name.equals("！")) {
                    writer.println();
                }

                if(pos==null || pos.equals("w")) {
                    continue;
                }

                writer.print(name);
                writer.print(" ");
            }
            writer.println();
            writer.flush();
        }
        counter.done();
        writer.close();
        newWordDictWriter.close();
        dump.close();
    }
//
//    private void testSegment() {
//        Conf conf = ConfFactory.defaultConf();
//        List<SegWord> list = SegmentFactory.getSegment(conf).tag("好啊，太好了，真棒！好好学习，天天向上！\n\r\n\n牢记毛主席的教诲。" + "继上次提取关键词之后，项目组长又要求我对关键词进行聚类。说实话，我不太明白对关键词聚类跟新闻推荐有什么联系，不过他说什么我照做就是了。\n" +
//                "\n" +
//                "按照一般的思路，可以用新闻ID向量来表示某个关键词，这就像广告推荐系统里面用用户访问类别向量来表示用户一样，然后就可以用kmeans的方法进行聚类了。不过对于新闻来说存在一个问题，那就量太大，如果给你十万篇新闻，那每一个关键词将需要十万维的向量表示，随着新闻数迅速增加，那维度就更大了，这计算起来难度太大。于是，这个方法思路简单但是不可行。");
//        System.out.println(list);
//    }

    public static void main(String[] args) throws ParseException, IOException {
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("c", true, "config file"));
        options.addOption(new Option("df", true, "wiki article dump file."));
        options.addOption(new Option("o", true, "output text file."));
        options.addOption(new Option("stat", false, "count how many articles in the dump file."));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("c")) {
            helpFormatter.printHelp("Usage:", options);
            return;
        }

        Conf conf = ConfFactory.createConf(commandLine.getOptionValue("c"), true);
        SegArticles2Txt segArticles2Txt = new SegArticles2Txt(conf);
        if (commandLine.hasOption("df") && commandLine.hasOption("o")) {
            String dumpFile = commandLine.getOptionValue("df");
            File out = new File(commandLine.getOptionValue("o"));
            WikiPageDump dump = new PageSequenceDump(conf, dumpFile);
            segArticles2Txt.segAndOut(dump, out);
        } else if(commandLine.hasOption("df") && commandLine.hasOption("stat")) {
            String dumpFile = commandLine.getOptionValue("df");
            WikiPageDump dump = new PageSequenceDump(conf, dumpFile);
            dump.open();
            int count = 0;
            while (dump.hasNext()) {
                WikiPage page = dump.next();
                if (page.isArticle()) {
                    count++;
                }
            }
            System.out.println("Total articles: " + count);
        } else {
            helpFormatter.printHelp("Usage:", options);
        }

        System.out.println("I'm DONE!");
    }
}
