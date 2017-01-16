package ruc.irm.wikit.db.je.struct;

import ruc.irm.wikit.db.je.Record;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Redirect Record
 *
 * @author Tian Xia
 * @date Feb 1, 2016 11:41 AM
 */
public class RedirectRecord extends Record {

    public enum RedirectType{
        Article,
        Category
    };

    private int fromId;
    private String from;
    private String to;
    private RedirectType type;

    public RedirectRecord(){}

    public RedirectRecord(int id, String from, String to, RedirectType type){
        this.fromId = id;
        this.type = type;
        this.from = from;
        this.to = to;
    }



    @Override
    protected void readFrom(ObjectInputStream input) throws IOException {
        this.fromId = input.readInt();
        this.from = input.readUTF();
        this.to = input.readUTF();
        int flag = input.readByte();
        if (flag == 0) {
            this.type = RedirectType.Article;
        } else if (flag == 1) {
            this.type = RedirectType.Category;
        }
    }

    @Override
    protected void writeIn(ObjectOutputStream out) throws IOException {
        out.writeInt(fromId);
        out.writeUTF(from);
        out.writeUTF(to);
        if (type == RedirectType.Article) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DbRedirect{");
        sb.append("fromId=").append(fromId);
        sb.append(", from='").append(from).append('\'');
        sb.append(", to='").append(to).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
