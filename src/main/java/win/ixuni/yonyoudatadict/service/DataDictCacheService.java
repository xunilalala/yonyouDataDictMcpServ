package win.ixuni.yonyoudatadict.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import win.ixuni.yonyoudatadict.cache.LRUCache;
import win.ixuni.yonyoudatadict.config.DataDictConfig;

/**
 * 数据字典缓存服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataDictCacheService {
    
    private final DataDictConfig config;
    private final LRUCache<String, Object> cache;
    
    /**
     * 下载数据字典详情，支持缓存
     * @param dictCode 字典编码
     * @return 字典详情数据
     */
    public Object downloadDataDictDetail(String dictCode) {
        // 如果缓存未启用，直接从网络获取
        if (!config.isCacheEnabled()) {
            log.debug("缓存未启用，直接从网络获取字典详情: {}", dictCode);
            return fetchFromNetwork(dictCode);
        }
        
        // 尝试从缓存获取
        Object cachedData = cache.get(dictCode);
        if (cachedData != null) {
            log.debug("从缓存获取字典详情: {}", dictCode);
            return cachedData;
        }
        
        // 缓存中没有，从网络获取并缓存
        log.debug("缓存未命中，从网络获取字典详情: {}", dictCode);
        Object data = fetchFromNetwork(dictCode);
        if (data != null) {
            cache.put(dictCode, data);
            log.debug("字典详情已缓存: {}", dictCode);
        }
        
        return data;
    }
    
    /**
     * 从网络获取数据字典详情
     * @param dictCode 字典编码
     * @return 字典详情数据
     */
    private Object fetchFromNetwork(String dictCode) {
        // TODO: 实现实际的网络请求逻辑
        // 这里应该调用实际的HTTP客户端来获取数据
        log.info("正在从网络获取字典详情: {}", dictCode);
        
        // 模拟网络请求
        try {
            Thread.sleep(100); // 模拟网络延迟
            return "字典详情数据_" + dictCode; // 模拟返回的数据
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取字典详情时发生中断: {}", dictCode, e);
            return null;
        }
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        if (config.isCacheEnabled()) {
            cache.clear();
            log.info("数据字典缓存已清空");
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        if (!config.isCacheEnabled()) {
            return "缓存未启用";
        }
        return String.format("缓存大小: %d/%d", cache.size(), config.getCacheSize());
    }
}
