package ruc.irm.wikit.cache;

import org.apache.commons.cli.*;
import ruc.irm.wikit.cache.impl.ArticleCacheRedisImpl;
import ruc.irm.wikit.cache.impl.CategoryCacheRedisImpl;
import ruc.irm.wikit.common.conf.Conf;
import ruc.irm.wikit.common.conf.ConfFactory;
import ruc.irm.wikit.common.exception.MissedException;
import ruc.irm.wikit.common.exception.WikitException;
import ruc.irm.wikit.data.dump.WikiPageDump;
import ruc.irm.wikit.data.dump.impl.PageSequenceDump;
import ruc.irm.wikit.data.dump.parse.WikiPage;
import ruc.irm.wikit.data.dump.parse.WikiPageFilter;

import java.io.IOException;
import java.util.Set;

/**
 * keep name and id bi-direction mapping, number id is used
 * to save memory instead of String name.
 *
 * @author Tian Xia
 * @date May 03, 2015 12:41 PM
 */
public interface NameIdMapping {
    void saveNameIdMapping(String name, int id);

    public Set<Integer> listIds();

    boolean nameExist(String name);

    boolean idExist(int id);

    /**
     * 根据名称返回对应的ID，如果该名称不存在，则返回valueForNotExisted
     * @param name
     * @param valueForNotExisted
     * @return
     */
    int getIdByName(String name, int valueForNotExisted);

    int getIdByName(String name) throws MissedException;

    String getNameById(int id) throws MissedException;

    String getNameById(int id, String defaultValue);

    /**
     * Finish process name-id mapping job, when finish to construct the
     * mapping work, this method should be called.
     */
    void finishNameIdMapping();

    /**
     * the name-id mapping relationship has created or not
     * @return
     */
    boolean nameIdMapped();

}
