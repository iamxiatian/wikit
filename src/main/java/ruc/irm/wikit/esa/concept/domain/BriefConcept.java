package ruc.irm.wikit.esa.concept.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * The concept brief information, note that the first concept's id is 1, then 2, 3...
 *
 * User: xiatian
 * Date: 4/11/14
 * Time: 7:02 PM
 */
public class BriefConcept {
    private int id;
    private String title;
    /** out id to keep the relationship with raw document */
    private String outId;

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

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("outId", outId)
                .append("title", title).toString();
    }

    public String getOutId() {
        return outId;
    }

    public void setOutId(String outId) {
        this.outId = outId;
    }
}
