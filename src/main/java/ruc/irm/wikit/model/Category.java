package ruc.irm.wikit.model;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Set;

/**
 * Wiki Category
 *
 * User: xiatian
 * Date: 4/14/14
 * Time: 11:36 PM
 */
public class Category {
    /** category's page id */
    private int pageId;

    /** category title */
    private String title;

    /** the parent category page id set of this category */
    private Set<Integer> parentIds;

    /** the pages belong to this category */
    private Set<Integer> articleIds;

    /** page count which belongs to this category */
    private int articleCount;

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<Integer> getParentIds() {
        return parentIds;
    }

    public void setParentIds(Set<Integer> parentIds) {
        this.parentIds = parentIds;
    }

    public Set<Integer> getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(Set<Integer> articleIds) {
        this.articleIds = articleIds;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    @Override
    public String toString() {
        return "Category{" +
                "pageId='" + pageId + '\'' +
                ", title='" + title + '\'' +
                ", parentIds=" + parentIds +
                ", articleIds=" + articleIds +
                ", articleCount=" + articleCount +
                '}';
    }

    public DBObject toDBObject() {
        BasicDBObject data = new BasicDBObject();
        data.append("pageId", pageId)
                .append("title", title)
                .append("parentIds", parentIds)
                .append("articleIds", articleIds)
                .append("articleCount", articleCount);
        return data;
    }
}
