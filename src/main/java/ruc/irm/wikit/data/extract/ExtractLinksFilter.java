package ruc.irm.wikit.data.extract;

import com.alibaba.fastjson.JSON;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;
import ruc.irm.wikit.util.Big5GB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
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
    private Conf conf;

    public ExtractLinksFilter(Conf conf, File linkOutFile) throws
            IOException {
        this.conf = conf;
        linkWriter = new BufferedWriter(new FileWriter(linkOutFile));
    }

    @Override
    public void process(WikiPage wikiPage, int index) {
        if (wikiPage.isArticle() && !wikiPage.isRedirect()) {
            int id = wikiPage.getId();
            String title = wikiPage.getTitle();
            if (conf.isBig5ToGb()) {
                title = Big5GB.toGB(title);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", title);

            List<String> internalLinks = new LinkedList<>();
            List<String> externalLinks = new LinkedList<>();
            for(Link link: wikiPage.getPageLinks()) {
                if(link.getType()==Link.type.INTERNAL) {
                    internalLinks.add(link.getText());
                    internalLinks.add(link.getTarget());
                } else if (link.getType() == Link.type.EXTERNAL) {
                    externalLinks.add(link.getText());
                    externalLinks.add(link.getTarget());
                }
            }

            map.put("ILS", internalLinks);
            map.put("ELS", externalLinks);
            String line = JSON.toJSONString(map);

            try {
                linkWriter.append(line).append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() throws IOException {
        linkWriter.close();
    }

}