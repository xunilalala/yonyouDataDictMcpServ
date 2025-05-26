package win.ixuni.yonyoudatadict.cache;


import win.ixuni.yonyoudatadict.config.DataDictConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个简单的基于 LinkedHashMap 实现的 LRU 缓存。
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity; // 缓存的最大容量

    /**
     * 构造函数
     */
    public LRUCache(DataDictConfig config) {
        // super(initialCapacity, loadFactor, accessOrder)
        // accessOrder = true 表示链表会按照访问顺序来维护，最近访问的元素会被移到末尾
        // accessOrder = false (默认) 表示链表会按照插入顺序来维护
        super(config.getCacheSize(), 0.75f, true);
        this.capacity = config.getCacheSize();
    }

    /**
     * 重写此方法来控制何时移除最老的条目。
     * 当 put 或 putAll 方法导致 map 中的元素数量超过了构造时指定的 capacity 时，
     * 此方法会被调用。如果返回 true，则最老的条目（最近最少使用的）会被移除。
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 当缓存大小超过指定容量时，返回 true，移除最老的条目
        return size() > capacity;
    }

}