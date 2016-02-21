package ruc.irm.wikit.data.extract;

import com.alibaba.fastjson.JSON;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import org.apache.commons.cli.*;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 从维基百科的词条中抽取出链接信息和重定向信息，链接信息保存到json格式的文件中，
 * 而重定向信息保存到了文本文件中，使用时通过命令行运行：<br/>
 *
 * ./run.py ExtractLinksFilter -c xxx.xml -lf links.json -rf redirect.txt
 *
 */
public class ExtractLabelsFilter implements WikiPageFilter {
    private BufferedWriter labelWriter = null;
    private Conf conf;

    public ExtractLabelsFilter(Conf conf, File outFile) throws
            IOException {
        this.conf = conf;
        labelWriter = new BufferedWriter(new FileWriter(outFile));
        labelWriter.append("from\tto\n");
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isArticle() && !wikiPage.isRedirect()) {
            int id = wikiPage.getId();
            String title = wikiPage.getTitle();
            if (conf.isBig5ToGb()) {
                title = Big5GB.toGB(title);
            }

            try {
                for (Link link : wikiPage.getPageLinks()) {
                    if (link.getType() == Link.type.INTERNAL) {
                        String anchor = link.getText();
                        String target = link.getTarget();
                        if (!anchor.equals(target)) {
                            labelWriter.append(anchor + "\t" + target + "\n");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        labelWriter.close();
    }

}