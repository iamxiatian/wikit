package ruc.irm.wikit.data.dump.parse;

import com.google.common.collect.Sets;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import de.tudarmstadt.ukp.wikipedia.parser.Paragraph;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.Section;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.util.Big5GB;
import ruc.irm.wikit.util.GZipUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: xiatian
 * Date: 4/4/14
 * Time: 1:44 AM
 */
public class WikiPage {
    private int id;
    private String title;
    private String ns;
    private String text;
    private String format;
    private String redirect;
    private String commonsCatTag = null;
    private boolean commonCategory = false;

    /**
     * 入链数量，默认为-1，表示没有该信息，如大于等于0，表示为统计出的真实数据.
     * 对于文章，该数值为入链，对于分类，该数值为拥有的文章数量
     */
    private int inlinkCount = -1;


    private String plainText = null;
    private Set<String> internalLinks = null;
    private Set<String> categories = null;

    /** 网页的别名，指向当前网页的redirect网页名称集合, 由于处理的需要，初始值设为空，
     * 后续处理时会补充该数据 */
    private Set<String> aliases = new HashSet<>();

    private Conf conf = null;

    public boolean isCommonCategory() {
       return commonCategory;
    }

    public WikiPage(Conf conf) {
        this.conf = conf;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (conf.isBig5ToGb()) {
            this.title = Big5GB.toGB(title);
        } else {
            this.title = title;
        }
    }

    public String getNs() {
        return ns;
    }

    public void setNs(String ns) {
        this.ns = ns;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (conf.isBig5ToGb()) {
            this.text = Big5GB.toGB(text);
        } else {
            this.text = text;
        }
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public boolean isRedirect() {
        return this.redirect!=null;
    }

    public int getInlinkCount() {
        return inlinkCount;
    }

    public void setInlinkCount(int inlinkCount) {
        this.inlinkCount = inlinkCount;
    }

    public boolean isArticle() {
        return StringUtils.isNotEmpty(text) && (StringUtils.isEmpty(ns) || ns.equals("0")) && "text/x-wiki".equals(format);
    }

    public boolean isCategory() {
        return this.ns.equals("14");
    }

    public String getCommonsCatTag() {
        return this.commonsCatTag;
    }

    public boolean isCommonsCatTag() {
        return this.commonsCatTag!=null;
    }

    public void drillMoreInfo() {
        internalLinks = new HashSet<>();
        categories = new HashSet<>();
        if(!isArticle() && !isCategory()) return;

        //parse category
        if (isCategory()) {
            commonCategory = true;
            //if it's root category, return as normal category
            if(!conf.getWikiRootCategoryName().equalsIgnoreCase
                    (getCategoryTitle())){
                //解析所隶属的类别
                categories = WikiTextParser.parseCategories(text);
                if(CollectionUtils.isEmpty(categories)) {
                    commonCategory = false;
                    //judge is a category redirect or not
                    this.redirect = WikiTextParser.parseCategoryRedirect(text);
                    if (!isRedirect()) {
                        this.commonsCatTag = WikiTextParser.parseCommonsCat(text);
                    }
                }
            }

            return;
        }

        categories = WikiTextParser.parseCategories(text);
        MediaWikiParserFactory pf = new MediaWikiParserFactory();
        MediaWikiParser parser = pf.createParser();
        ParsedPage pp = parser.parse(text);
        if (pp == null) {
            plainText = "";
            System.out.println("text parse error: id==>" + id + ", title==>" + title + ", ns==>" + ns + ", content==>" + text);
            return;
        }

        plainText = "";
        for (Section s : pp.getSections()) {
            if (s.getTitle() != null) {
                plainText += s.getTitle() + "\n";
            }

            for (Paragraph p : s.getParagraphs()) {
                String par = p.getText();
                if (par.startsWith("TEMPLATE")) {
                    continue;
                }
                if (par.matches("[^:]+:[^\\ ]+")) {
                    continue;
                }
                plainText += par + "\n\n";
            }
        }


        for (Link link : pp.getLinks()) {
            if (link.getType() == Link.type.INTERNAL) {
                internalLinks.add(link.getTarget());
            }
        }
    }

    public String getPlainText() {
        if (plainText == null) {
            StringBuilder sb = new StringBuilder();

            MediaWikiParserFactory pf = new MediaWikiParserFactory();
            MediaWikiParser parser = pf.createParser();
            ParsedPage pp = parser.parse(text);
            if (pp != null) {
                for (Section s : pp.getSections()) {
                    if (s.getTitle() != null) {
                        sb.append(s.getTitle()).append("\n");
                        //plainText += s.getTitle() + "\n";
                    }

                    for (Paragraph p : s.getParagraphs()) {
                        String par = p.getText();
                        if (par.startsWith("TEMPLATE")) {
                            continue;
                        }
                        if (par.matches("[^:]+:[^\\ ]+")) {
                            continue;
                        }

                        //处理内容里面是否包含TEMPLATE
                        Pattern pattern = Pattern.compile("TEMPLATE\\[[^\\]]+\\]", Pattern
                                .CASE_INSENSITIVE);
                        Matcher matcher = pattern.matcher(par);
                        int last = 0;
                        while(matcher.find(last)) {
                            sb.append(par.substring(last, matcher.start()));
                            last = matcher.end();
                        }
                        sb.append(par.substring(last));
                        sb.append("\n\n");
                        //plainText += par + "\n\n";
                        //sb.append(par).append("\n\n");
                    }
                }
            }

            plainText = sb.toString();
        }
        return plainText;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public Set<String> getInternalLinks() {
        return this.internalLinks;
    }

    public void setInternalLinks(Set<String> internalLinks) {
        this.internalLinks = internalLinks;
    }

    public String getCategoryTitle() {
        if(this.title.toLowerCase().startsWith("category:")) {
            return this.title.substring("Category:".length());
        } else if (this.title.equals("页面分类:")) {
            return this.title.substring("页面分类:".length());
        } else {
            return title;
        }
    }

    public static WikiPage readFromMongo(Conf conf, Document record) {
        WikiPage wikiPage = new WikiPage(conf);

        wikiPage.setId(record.getInteger("id"));
        wikiPage.setNs(record.getString("ns"));
        wikiPage.setTitle(record.getString("title"));
        wikiPage.setText(GZipUtils.unzip((byte[]) record.get("text"), "utf-8"));
        wikiPage.setFormat(record.getString("format"));
        wikiPage.setRedirect(record.getString("redirect"));
        wikiPage.setInternalLinks(Sets.newConcurrentHashSet(
                (Collection<String >)record.get("links")));
        return wikiPage;
    }


    public void writeIn(DataOutputStream dos) throws IOException {
        //write page type: 0 for article, 1 for category
        if (isArticle()) {
            dos.writeByte(0);
        } else if (isCommonCategory()) {
            //common category
            dos.writeByte(1);
        } else if (isCommonsCatTag()) {
            //not command category
            dos.writeByte(2);
        } else {
            throw new IOException("The page can not be written to file:" + toString());
        }

        dos.writeInt(getId());
        dos.writeUTF(title == null ? "" : title);
        dos.writeUTF((redirect == null) ? "" : redirect);
        dos.writeUTF((commonsCatTag == null) ? "" : commonsCatTag);

        byte[] textBuffer = text.getBytes("utf-8");
        dos.writeInt(textBuffer.length);
        dos.write(textBuffer, 0, textBuffer.length);

        dos.writeInt(categories.size());
        for (String c : categories) {
            dos.writeUTF(c);
        }

        dos.writeInt(getAliases().size());
        for (String alias : getAliases()) {
            dos.writeUTF(alias);
        }


        dos.writeInt(internalLinks.size());
        for (String link : internalLinks) {
            dos.writeUTF(link);
        }

        dos.writeInt(inlinkCount);
        dos.flush();
    }

    public static WikiPage readFrom(DataInputStream dis, final Conf conf) {
        try {
            byte type = dis.readByte();
            WikiPage page = new WikiPage(conf);

            if (type == 0) {
                page.ns = "0";
            } else if (type == 1){
                page.ns = "14";
                page.commonCategory = true;
            } else if(type == 2) {
                page.ns = "14";
                page.commonCategory = false;
            } else {
                throw new IOException("Unknown Wiki page type " + type);
            }

            page.format = "text/x-wiki";

            page.id = dis.readInt();
            page.title = dis.readUTF();

            String redirect = dis.readUTF();
            page.redirect = redirect.isEmpty()?null:redirect;

            String commonsCatTag = dis.readUTF();
            page.commonsCatTag = commonsCatTag.isEmpty()?null:commonsCatTag;

            byte[] buffer = new byte[dis.readInt()];
            dis.readFully(buffer);
            page.text = new String(buffer, "utf-8");

            Set<String> categories = new HashSet<>();
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                categories.add(dis.readUTF());
            }
            page.categories = categories;

            Set<String> aliases = new HashSet<>();
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                aliases.add(dis.readUTF());
            }
            page.aliases = aliases;


            Set<String> links = new HashSet<>();
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                links.add(dis.readUTF());
            }
            page.internalLinks = links;

            page.inlinkCount = dis.readInt();
            return page;
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "WikiPage{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", ns='" + ns + '\'' +
                ", format='" + format + '\'' +
                ", redirect='" + redirect + '\'' +
                ", categories='" + categories + '\'' +
                //", text='" + text + '\'' +
                '}';
    }

}
