package ruc.irm.wikit.data.dump.filter;

import org.apache.commons.lang3.StringUtils;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.data.cache.ArticleCache;
import ruc.irm.wikit.data.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter is to make a sample of wiki articles for experiment purpose.
 *
 * The structure of sample file is organized by XML:
 * <pre>
 * <article>
 *      <id>123</id>
 *      <title>title</title>
 *      <category>category1</category>
 *      <category>category2...</category>
 *      <body>content</body>
 * </article>
 *</pre>
 *
 */
public class SampleFilter implements WikiPageFilter {
    private Conf conf = null;
    private int totalPages = 0;
    private int normalArticles = 0;
    private int sampleCount = 0;
    private int sampleRatioPerThousand = 15; //sample ratio per thousand

    private ArticleCache articleCache = null;
    private OutputStream out = null;
    private XMLStreamWriter writer = null;

    /**
     * minimum non-stop words for valid wiki page
     */
    private int minWords = 200;
    private Random random = new Random(System.currentTimeMillis());

    public SampleFilter(Conf conf) throws IOException, XMLStreamException {
        this.conf = conf;

        this.sampleRatioPerThousand = conf.getInt("expt.wiki.sample.ratio", 30);

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

        File sampleFile = new File(conf.getDataDir(),
                "sample-" + df.format(new Date()) + ".xml");

        this.articleCache = new ArticleCacheRedisImpl(conf);

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        this.out = new FileOutputStream(sampleFile);
        this.writer = factory.createXMLStreamWriter(out);
        this.writer.writeStartDocument("utf-8", "1.0");
        writer.writeCharacters("\n");
        writer.writeStartElement("wiki-articles");
    }


    private boolean accept(WikiPage wikiPage) throws IOException {
        //step 1: if title starts with 4 digits, then skip
        String title = wikiPage.getTitle().toLowerCase();
        if (wikiPage.isCategory()) {
            title = wikiPage.getCategoryTitle().toLowerCase();
        }
        if (title.length() > 7) {
            //保留1980s此类词条
            String startString = title.substring(0, 4);
            if (StringUtils.isNumeric(startString)) {
                return false;
            }
        }

        //step 2: remove "list of xxxx" and "index of xxx"
        if (title.indexOf("index of ") >= 0 || title.indexOf("list of") >= 0
                || title.indexOf("(disambiguation)")>=0) {
            return false;
        }

        if (title.indexOf("列表") >= 0 || title.indexOf("歧义") >= 0) {
            return false;
        }

        //以年份结尾的词条，符合年份时代结尾的形式文章，如``China national football team results (2000–09)''，因为这类文章的作用更类似于类别，起到信息组织的作用。
        Pattern pattern = Pattern.compile("\\(\\d{4}(–|\\-)\\d{2,4}\\)$");
        if (pattern.matcher(title).find()) {
            return false;
        }

        //去除类别为Disambiguation pages的词条
        Set<String> categories = wikiPage.getCategories();
        if (categories.size() < 3) {
            for (String c : categories) {
                if(c.equalsIgnoreCase("Disambiguation pages")){
                    return false;
                }
            }
        }

        //step 2: check token numbers
        if(wikiPage.getPlainText().length()<60){
            return false;
        }

        return true;
    }

    @Override
    public void process(WikiPage wikiPage, final int index) {
        totalPages++;

        wikiPage.drillMoreInfo();

        //only common category and articles are saved
        if (wikiPage.isArticle()) {
            try {
                if (!wikiPage.isRedirect()) {
                    normalArticles++;
                    String title = wikiPage.getTitle();

                    int p = random.nextInt(1000);
                    //sample by 1 percent
                    if(p < sampleRatioPerThousand && accept(wikiPage)
                            && !articleCache.nameExist(title)) {
                        //skip the wiki if it still contains "TEMPLATE",
                        // although that is strange if the content has this
                        // substring.
                        String snippet = getSnippet(wikiPage.getPlainText());
                        if(snippet.length()<50 || snippet.contains("TEMPLATE[")){
                            totalPages++;
                            return;
                        }

                        writer.writeCharacters("\n\t");
                        writer.writeStartElement("wiki");
                        writer.writeAttribute("id", Integer.toString(wikiPage.getId()));


                        writer.writeCharacters("\n\t\t");
                        writer.writeStartElement("title");
                        writer.writeCharacters(title);
                        writer.writeEndElement(); //end title


                        for (String c : wikiPage.getCategories()) {
                            writer.writeCharacters("\n\t\t");
                            writer.writeStartElement("category");
                            writer.writeCharacters(c);
                            writer.writeEndElement(); //end title
                        }


                        writer.writeCharacters("\n\t\t");
                        writer.writeStartElement("body");
                        writer.writeCData(snippet);
                        writer.writeEndElement();

                        writer.writeCharacters("\n\t");
                        writer.writeEndElement();
                        writer.writeCharacters("\n"); //end wiki

                        sampleCount++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(totalPages%10000==0) {
            System.out.println("Parsed " + totalPages + " pages.");
        }
    }

    private String getSnippet(String text) {
        StringBuilder sb = new StringBuilder();

        int charCount = 0;
        if(text.length()<100) return text;

        for(String line:StringUtils.split(text, "\n")) {
            //处理内容里面是否包含TEMPLATE
            Pattern pattern = Pattern.compile("TEMPLATE\\[[^\\]]+\\]", Pattern
                    .CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            int last = 0;
            while(matcher.find(last)) {
                sb.append(line.substring(last, matcher.start()));

                charCount += (matcher.start() - last);
                last = matcher.end();
            }
            sb.append(line.substring(last));
            charCount += (line.length() - last);

            sb.append(line).append("\t");
            if(charCount>150) break;
        }
        return sb.toString().trim();
    }

    public void close() throws IOException {
        try {
            writer.writeCharacters("\n");
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            out.close();
            System.out.println("Sampled " + sampleCount + " items from " +
                    normalArticles + " normal articles");
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }
}
