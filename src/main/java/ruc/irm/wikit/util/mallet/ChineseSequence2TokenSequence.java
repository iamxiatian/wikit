package ruc.irm.wikit.util.mallet;

import cc.mallet.extract.StringTokenization;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.SingleInstanceIterator;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pipe that tokenizes a Chinese character sequence.  Expects a CharSequence
 * in the Instance data, and converts the sequence into a token
 * sequence using Chinese segmentation
 */
public class ChineseSequence2TokenSequence extends Pipe implements Serializable {
    private Set<String> denydPosSet = new HashSet<String>();

    public ChineseSequence2TokenSequence() {

    }

    public ChineseSequence2TokenSequence(String[] denyPosArray) {
        if (denyPosArray != null) {
            for (String pos : denyPosArray) {
                this.denydPosSet.add(pos);
            }
        }
    }

    public Instance pipe(Instance carrier) {
        CharSequence string = (CharSequence) carrier.getData();
        TokenSequence ts = new StringTokenization(string);

        List<Term> terms = HanLP.segment(string.toString());

        if(denydPosSet.isEmpty()) {
            for (Term term : terms) {
                if(term.word.length()>=2)
                    ts.add(new Token(term.word));
            }
        } else {
            for (Term term : terms) {
                if(term.word.length()>=2 && !denydPosSet.contains(term.nature.name()))
                   ts.add(new Token(term.word));
            }
        }
        carrier.setData(ts);
        return carrier;
    }

    public static void main(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                Instance carrier = new Instance(new File(args[i]), null, null, null);
                SerialPipes p = new SerialPipes(new Pipe[]{
                        new Input2CharSequence(),
                        new ChineseSequence2TokenSequence()});
                carrier = p.newIteratorFrom(new SingleInstanceIterator(carrier)).next();
                TokenSequence ts = (TokenSequence) carrier.getData();
                System.out.println("===");
                System.out.println(args[i]);
                System.out.println(ts.toString());
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
    }


}
