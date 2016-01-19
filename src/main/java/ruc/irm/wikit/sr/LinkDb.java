package ruc.irm.wikit.sr;

import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;

import java.util.List;

/**
 * The DB which provide link access method.
 *
 * @author Tian Xia
 * @date Jan 19, 2016 10:37 AM
 */
public interface LinkDb {
    public TIntSet getInlinks(int pageId);

    public TIntSet getOutLinks(int pageId);

    public int getTotalPages();
}
