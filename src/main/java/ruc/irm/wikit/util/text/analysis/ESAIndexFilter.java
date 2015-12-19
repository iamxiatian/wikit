package ruc.irm.wikit.util.text.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.util.Version;
import ruc.irm.wikit.util.TextUtils;

/**
 * User: xiatian
 * Date: 3/25/14
 * Time: 10:36 PM
 */
public class ESAIndexFilter extends FilteringTokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    /**
     * This will filter out tokens whose
     * {@link CharTermAttribute} is either too short ({@link CharTermAttribute#length()}
     * &lt; min) or too long ({@link CharTermAttribute#length()} &gt; max).
     * @param version the Lucene match version
     * @param in      the {@link TokenStream} to consume
     */
    public ESAIndexFilter(Version version, TokenStream in) {
        super(version, in);
    }

    @Override
    public boolean accept() {
        final int len = termAtt.length();
        String term = termAtt.toString();
        return accept(term);
    }

    public boolean accept(String source) {
        return !TextUtils.isNumberOrDate(source);
    }

}