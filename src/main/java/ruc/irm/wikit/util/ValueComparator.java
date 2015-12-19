package ruc.irm.wikit.util;

import java.util.Comparator;
import java.util.Map;

/**
 * User: xiatian
 * Date: 5/8/14
 * Time: 10:19 AM
 */
public class ValueComparator<T, V extends Number> implements Comparator<T> {
    private Map<T, V> base = null;

    public ValueComparator(Map<T, V> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.
    public int compare(T a, T b) {
        V value1 = base.get(a);
        V value2 = base.get(b);
        if (value1.doubleValue() >= value2.doubleValue()) {
            return -1;
        } else {
            return 1;
        }
        // returning 0 would merge keys
    }
}
