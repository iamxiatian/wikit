package ruc.irm.wikit.nlp.word2vec;

import com.google.common.base.Splitter;
import org.apache.commons.io.IOUtils;
import org.deeplearning4j.text.sentenceiterator.BaseSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;

import java.util.Iterator;

/**
 * @author Tian Xia
 * @date Jul 06, 2016 11:20
 */
public class WikiSentenceIterator extends BaseSentenceIterator {
    private WikiPageDump dump = null;
    private Iterator<String> lineIterator = null;

    public WikiSentenceIterator(WikiPageDump dump) {
        this.dump = dump;
    }

    private boolean readMore() {
        while(dump.hasNext()) {
            WikiPage wikPage = dump.next();
            if(!wikPage.isArticle()) {
                continue;
            }
            String text = wikPage.getPlainText();
            lineIterator = Splitter.on("\n").splitToList(text).iterator();

            if(!lineIterator.hasNext()) continue;

            return true;
        }
        return false;
    }

    @Override
    public String nextSentence() {
        return lineIterator.next();
    }

    @Override
    public boolean hasNext() {
        if (lineIterator == null || !lineIterator.hasNext()) {
            if (!readMore()) {
                return false;
            }
        }
        return lineIterator.hasNext();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }
}
