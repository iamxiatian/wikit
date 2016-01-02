package ruc.irm.wikit.db.je;

import java.io.*;

/**
 * @author Tian Xia
 * @date Dec 26, 2015 12:40 AM
 */
public abstract class Record {
    public void readFrom(byte[] bytes) throws IOException{
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes));
        readFrom(input);
        input.close();
    };

    protected abstract void readFrom(ObjectInputStream input) throws
            IOException;

    public byte[] toByteArray() throws IOException{
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ObjectOutputStream outObject = new ObjectOutputStream(outBytes);
        writeIn(outObject);
        outObject.flush();
        outBytes.flush();
        outBytes.close();
        return outBytes.toByteArray();
    }

    protected abstract void writeIn(ObjectOutputStream out) throws
            IOException;
}
