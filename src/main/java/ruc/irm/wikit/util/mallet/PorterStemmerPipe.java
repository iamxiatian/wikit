package ruc.irm.wikit.util.mallet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;
import ruc.irm.wikit.util.PorterStemmer;

public class PorterStemmerPipe extends Pipe {

    private static final long serialVersionUID = 154100332101873830L;

    public Instance pipe(Instance carrier) {
        TokenSequence ts = (TokenSequence) carrier.getData();
        String word;
        PorterStemmer s;

        for (int i = 0; i < ts.size(); i++) {
            word = ts.get(i).getText();
            //stem the word
            s = new PorterStemmer();
            for (char ch : word.toCharArray()) {
                if (Character.isLetter(ch)) {
                    s.add(ch);
                }
            }
            s.stem();
            ts.get(i).setText(s.toString());
        }
        carrier.setData(ts);

        return carrier;
    }

}
