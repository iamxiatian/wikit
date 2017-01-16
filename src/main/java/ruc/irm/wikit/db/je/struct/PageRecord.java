package ruc.irm.wikit.db.je.struct;

import org.apache.commons.lang3.builder.ToStringBuilder;
import ruc.irm.wikit.db.je.Record;
import ruc.irm.wikit.util.GZipUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Page Record
 *
 * @author Tian Xia
 * @date Dec 25, 2015 11:41 AM
 */
public class PageRecord extends Record {
    private int id;
    private String title;

    public String getText() {
        return text;
    }

    private String text;
    private int type;

    public PageRecord(){}

    public PageRecord(int id, String title, int type, String text){
        this.id = id;
        this.type = type;
        this.title = title;
        this.text = text;
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
        this.title = title;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    protected void readFrom(ObjectInputStream input) throws IOException {
        this.id = input.readInt();
        this.type = input.readInt();
        this.title = input.readUTF();

        //for long text, we zipped it.
        boolean gzipped = input.readBoolean();
        if(gzipped) {
            int len = input.readInt();
            byte[] buf = new byte[len];
            input.readFully(buf);
            this.text = GZipUtils.unzip(buf, "utf-8");
        } else{
            this.text = input.readUTF();
        }
    }

    @Override
    protected void writeIn(ObjectOutputStream out) throws IOException {
        out.writeInt(id);
        out.writeInt(type);
        out.writeUTF(title);
        //gzip long text
        if(text!=null && text.length()>100) {
            out.writeBoolean(true);
            byte[] buf = GZipUtils.gzip(text, "utf-8");
            out.writeInt(buf.length);
            out.write(buf);
        } else{
            out.writeBoolean(false);
            out.writeUTF(text == null ? "" : text);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("title", title)
                .append("text", text)
                .append("type", type)
                .toString();
    }
}
