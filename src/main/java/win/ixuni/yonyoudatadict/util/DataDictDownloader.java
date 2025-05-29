package win.ixuni.yonyoudatadict.util;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import win.ixuni.yonyoudatadict.adapter.VersionAdapter;
import win.ixuni.yonyoudatadict.adapter.VersionAdapterFactory;
import win.ixuni.yonyoudatadict.cache.LRUCache;
import win.ixuni.yonyoudatadict.config.DataDictConfig;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.model.YonyouVersion;
import win.ixuni.yonyoudatadict.processor.CustomFieldRemovalProcessor;
import win.ixuni.yonyoudatadict.processor.DataDictProcessor;
import win.ixuni.yonyoudatadict.processor.DefaultDataDictProcessor;
import win.ixuni.yonyoudatadict.processor.RefClassPathHrefProcessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用友数据字典下载工具类 - 多版本适配架构
 */
@Component
public class DataDictDownloader {
    
    private static final Logger logger = LoggerFactory.getLogger(DataDictDownloader.class);
    
    private final DataDictConfig config;
    private final RestTemplate restTemplate;

    private final VersionAdapterFactory adapterFactory;
    
    // 处理器链
    private List<DataDictProcessor> processors = new ArrayList<>();

    // 数据字典项列表缓存
    private volatile List<DataDictItem> dataDictItemsCache = null;

    /**
     * -- GETTER --
     * 获取DataDictDownloader实例的静态方法
     * 供处理器使用，避免循环依赖
     */
    // 静态实例引用，用于处理器访问
    @Getter
    private static DataDictDownloader instance;

    // 数据字典详情LRU缓存
    private final LRUCache<String, DataDictDetail> detailCache;
    
    @Autowired
    public DataDictDownloader(DataDictConfig config, VersionAdapterFactory adapterFactory) {
        this.config = config;
        this.adapterFactory = adapterFactory;
        this.restTemplate = new RestTemplate();

        // 初始化详情缓存
        this.detailCache = new LRUCache<>(config);

        // 设置RestTemplate使用UTF-8编码
        this.restTemplate.getMessageConverters().clear();
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        this.restTemplate.getMessageConverters().add(stringConverter);

        // 重新添加其他必要的转换器
        this.restTemplate.getMessageConverters().addAll(
                new RestTemplate().getMessageConverters().stream()
                        .filter(converter -> !(converter instanceof StringHttpMessageConverter))
                        .collect(java.util.stream.Collectors.toList())
        );
        
        // 添加默认处理器
        this.processors.add(new DefaultDataDictProcessor());
        // 添加RefClassPathHref处理器
        this.processors.add(new RefClassPathHrefProcessor());
        // 添加自定义字段移除处理器
        this.processors.add(new CustomFieldRemovalProcessor(config));

        // 设置静态实例
        instance = this;

        // 检测当前版本
        String currentAppCode = config.getDefaultAppCode();
        if (currentAppCode != null && !currentAppCode.trim().isEmpty()) {
            YonyouVersion version = adapterFactory.detectVersion(currentAppCode);
            logger.info("当前配置版本: {} ({})", version.getDisplayName(), version.getParseType());
        }
    }

    /**
     * 清除详情缓存
     */
    public void clearDetailCache() {
        if (detailCache != null) {
            synchronized (detailCache) {
                detailCache.clear();
                logger.info("数据字典详情缓存已清除");
            }
        }
    }

    /**
     * 下载并解析数据字典详情
     *
     * @param classId         类ID
     * @param applyProcessors 是否应用处理器链
     * @return 数据字典详情
     */
    public DataDictDetail downloadDataDictDetail(String classId, boolean applyProcessors) {
        // 先检查缓存
        if (config.isCacheEnabled()) {
            synchronized (detailCache) {
                DataDictDetail cachedDetail = detailCache.get(classId);
                if (cachedDetail != null) {
                    logger.info("从缓存返回数据字典详情，classId: {}", classId);
                    return cachedDetail;
                }
            }
        }

        try {
            String currentAppCode = config.getDefaultAppCode();
            if (currentAppCode == null || currentAppCode.trim().isEmpty()) {
                logger.error("默认应用代码 (default-app-code) 未在配置文件中设置");
                return null;
            }

            // 获取对应版本的适配器
            VersionAdapter adapter = adapterFactory.getAdapter(currentAppCode);
            if (adapter == null) {
                logger.error("无法找到适合的版本适配器，应用代码: {}", currentAppCode);
                return null;
            }

            String url = adapter.buildDetailUrl(config.getBaseUrl(), currentAppCode, classId);
            logger.info("使用 {} 下载数据字典详情，URL: {}",
                    adapter.getSupportedVersion().getDisplayName(), url);

            String content = downloadWithProperEncoding(url, adapter);
            if (content == null) {
                logger.error("无法下载数据字典详情");
                return null;
            }

            DataDictDetail detail = adapter.parseDataDictDetail(content, classId);

            // 根据参数决定是否应用处理器链
            if (applyProcessors && detail != null) {
                // 应用处理器链
                for (DataDictProcessor processor : processors) {
                    detail = processor.process(detail);
                    if (detail == null) {
                        break;
                    }
                }
            }

            // 缓存结果（只缓存经过完整处理的结果）
            if (detail != null && config.isCacheEnabled() && applyProcessors) {
                synchronized (detailCache) {
                    detailCache.put(classId, detail);
                    logger.info("数据字典详情已缓存，classId: {}", classId);
                }
            }

            return detail;
        } catch (Exception e) {
            logger.error("下载或解析数据字典详情时出错", e);
            return null;
        }
    }
    
    /**
     * 注册数据字典处理器
     * 
     * @param processor 数据字典处理器
     * @return 当前实例，支持链式调用
     */
    public DataDictDownloader registerProcessor(DataDictProcessor processor) {
        if (processor != null) {
            this.processors.add(processor);
        }
        return this;
    }
    
    /**
     * 获取所有注册的处理器
     */
    public List<DataDictProcessor> getProcessors() {
        return new ArrayList<>(processors);
    }
    
    /**
     * 清除所有处理器
     */
    public void clearProcessors() {
        this.processors.clear();
        // 重新添加默认处理器
        this.processors.add(new DefaultDataDictProcessor());
    }
    
    /**
     * 下载并解析数据字典
     *
     * @return 数据字典项列表
     */
    public List<DataDictItem> downloadDataDictItems() {
        boolean cacheEnabled = config.isCacheEnabled();

        if (cacheEnabled) {
            if (dataDictItemsCache != null) {
                logger.info("从缓存返回数据字典项列表");
                return new ArrayList<>(dataDictItemsCache);
            }
        }

        try {
            String currentAppCode = config.getDefaultAppCode();
            if (currentAppCode == null || currentAppCode.trim().isEmpty()) {
                logger.error("默认应用代码 (default-app-code) 未在配置文件中设置");
                return new ArrayList<>();
            }

            // 获取对应版本的适配器
            VersionAdapter adapter = adapterFactory.getAdapter(currentAppCode);
            if (adapter == null) {
                logger.error("无法找到适合的版本适配器，应用代码: {}", currentAppCode);
                return new ArrayList<>();
            }

            String url = adapter.buildDictListUrl(config.getBaseUrl(), currentAppCode);
            logger.info("使用 {} 下载数据字典，URL: {}",
                    adapter.getSupportedVersion().getDisplayName(), url);

            String content = downloadWithProperEncoding(url, adapter);
            if (content == null) {
                logger.error("无法下载数据字典内容");
                return new ArrayList<>();
            }

            List<DataDictItem> items = adapter.parseDataDictItems(content);

            if (cacheEnabled) {
                synchronized (this) {
                    // 双重检查锁定，确保只初始化一次
                    if (dataDictItemsCache == null) {
                        this.dataDictItemsCache = new ArrayList<>(items);
                        logger.info("数据字典项列表已缓存");
                    }
                }
            }
            return items;
        } catch (Exception e) {
            logger.error("下载或解析数据字典时出错", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 使用正确编码下载内容
     */
    private String downloadWithProperEncoding(String url, VersionAdapter adapter) {
        try {
            // 设置请求头，模拟浏览器请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Charset", "UTF-8,GBK;q=0.7,*;q=0.3");
            headers.set("Origin", "https://www.oyonyou.com");
            headers.set("Referer", "https://www.oyonyou.com/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36");

            // 添加版本特定的请求头
            if (adapter.needsSpecialHeaders()) {
                Map<String, String> specialHeaders = adapter.getSpecialHeaders();
                for (Map.Entry<String, String> entry : specialHeaders.entrySet()) {
                    headers.set(entry.getKey(), entry.getValue());
                }
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 直接使用String类型获取响应，RestTemplate会使用UTF-8编码处理
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String content = response.getBody();

            if (content != null) {
                logger.info("成功获取内容，长度: {}, 内容类型: {}",
                        content.length(), adapter.getContentType());
                return content;
            }

            return null;

        } catch (Exception e) {
            logger.error("下载内容时出错: {}", url, e);
            return null;
        }
    }

    /**
     * 下载并解析数据字典详情
     *
     * @param classId 类ID
     * @return 数据字典详情
     */
    public DataDictDetail downloadDataDictDetail(String classId) {
        return downloadDataDictDetail(classId, true);
    }

    /**
     * 获取详情缓存大小
     */
    public int getDetailCacheSize() {
        if (detailCache != null) {
            synchronized (detailCache) {
                return detailCache.size();
            }
        }
        return 0;
    }

    /**
     * 获取当前版本信息
     */
    public YonyouVersion getCurrentVersion() {
        String currentAppCode = config.getDefaultAppCode();
        return adapterFactory.detectVersion(currentAppCode);
    }

    /**
     * 获取支持的所有版本
     */
    public Map<YonyouVersion, VersionAdapter> getSupportedVersions() {
        return adapterFactory.getAllAdapters();
    }
}