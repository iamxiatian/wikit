package ruc.irm.wikit.util;

import java.util.HashMap;

/**
 * @author Tian Xia
 * @date Jan 23, 2016 12:30 AM
 */
public class ExtendMap<K, V> extends HashMap<K, V> {
    public ExtendMap append(K key, V value) {
        put(key, value);
        return this;
    }

    public static ExtendMap<String, Object> newMap() {
        return new ExtendMap<String, Object>();
    }
}
