package ruc.irm.wikit.esa.concept.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: xiatian
 * Date: 4/11/14
 * Time: 7:02 PM
 */
public class FullConcept extends BriefConcept {

    /** plain text content, this field will be used for indexing process */
    private String plainContent;

    /** raw content for display */
    private String rawContent;

    /** out links: the different concept title occurred in this concept */
    private Collection<String> outLinks = new ArrayList<>();

    private Collection<String> aliasNames = new HashSet<>();

    private Set<String> categories = new HashSet<>();

    public Collection<String> getOutLinks() {
        return outLinks;
    }

    public void setOutLinks(Collection<String> outLinks) {
        this.outLinks = outLinks;
    }

    public String getPlainContent() {
        return plainContent;
    }

    public void setPlainContent(String plainContent) {
        this.plainContent = plainContent;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Collection<String> getAliasNames() {
        return aliasNames;
    }

    public void setAliasNames(Collection<String> aliasNames) {
        this.aliasNames = aliasNames;
    }
}
