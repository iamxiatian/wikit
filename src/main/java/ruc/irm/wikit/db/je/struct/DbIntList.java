package ruc.irm.wikit.db.je.struct;

import ruc.irm.wikit.db.je.Record;
import ruc.irm.wikit.util.NumberUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tian Xia
 * @date Jan 02, 2016 11:11 AM
 */
public class DbIntList extends Record {
    private List<Integer> ids = new ArrayList<>();

    @Override
    protected void readFrom(ObjectInputStream input) throws IOException {
        ids.clear();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            ids.add(input.readInt());
        }
    }

    @Override
    protected void writeIn(ObjectOutputStream out) throws IOException {
        out.writeInt(ids.size());
        for (int id : ids) {
            out.writeInt(id);
        }
    }

    public List<Integer> getIds() {
        return this.ids;
    }
}
