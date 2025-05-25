package win.ixuni.yonyoudatadict.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import win.ixuni.yonyoudatadict.config.DataDictConfig;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.processor.CustomFieldRemovalProcessor;
import win.ixuni.yonyoudatadict.processor.DataDictProcessor;
import win.ixuni.yonyoudatadict.processor.DefaultDataDictProcessor;
import win.ixuni.yonyoudatadict.processor.RefClassPathHrefProcessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用友数据字典下载工具类
 */
@Component
public class DataDictDownloader {
    
    private static final Logger logger = LoggerFactory.getLogger(DataDictDownloader.class);
    
    private final DataDictConfig config;
    private final RestTemplate restTemplate;
    
    // 用于提取JS中的JSON数组的正则表达式
    private static final Pattern DATA_PATTERN = Pattern.compile("var\\s+dataDictIndexData\\s*=\\s*(\\[.*?\\])\\s*;", Pattern.DOTALL);
    
    // 数据字典详情API路径模板
    private static final String DETAIL_API_TEMPLATE = "/%s/dict/%s.json";
    
    // 处理器链
    private List<DataDictProcessor> processors = new ArrayList<>();

    // 数据字典项列表缓存
    private volatile List<DataDictItem> dataDictItemsCache = null;
    
    @Autowired
    public DataDictDownloader(DataDictConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();

        // 设置RestTemplate使用UTF-8编码
        this.restTemplate.getMessageConverters()
                .stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        // 添加默认处理器
        this.processors.add(new DefaultDataDictProcessor());
        // 添加RefClassPathHref处理器
        this.processors.add(new RefClassPathHrefProcessor());
        // 添加自定义字段移除处理器
        this.processors.add(new CustomFieldRemovalProcessor(config));
    }
    
    /**
     * 下载并解析数据字典详情
     *
     * @param classId 类ID
     * @return 数据字典详情
     */
    public DataDictDetail downloadDataDictDetail(String classId) {
        try {
            String currentAppCode = config.getDefaultAppCode();
            if (currentAppCode == null || currentAppCode.trim().isEmpty()) {
                logger.error("默认应用代码 (default-app-code) 未在配置文件中设置");
                return null;
            }
            String url = buildDetailUrl(currentAppCode, classId);
            logger.info("下载数据字典详情，URL: {}", url);

            String jsonContent = downloadWithProperEncoding(url);
            if (jsonContent == null) {
                logger.error("无法下载数据字典详情");
                return null;
            }

            DataDictDetail detail = parseDetailJson(jsonContent, classId);

            // 应用处理器链
            for (DataDictProcessor processor : processors) {
                detail = processor.process(detail);
                if (detail == null) {
                    break;
                }
            }

            return detail;
        } catch (Exception e) {
            logger.error("下载或解析数据字典详情时出错", e);
            return null;
        }
    }
    
    /**
     * 构建完整的URL
     */
    private String buildUrl(String appCode) {
        return config.getBaseUrl() + "/" + appCode + config.getStaticPath();
    }
    
    /**
     * 解析JS内容，提取数据字典项
     */
    private List<DataDictItem> parseJsContent(String jsContent) {
        List<DataDictItem> result = new ArrayList<>();
        
        Matcher matcher = DATA_PATTERN.matcher(jsContent);
        if (matcher.find()) {
            String jsonArrayString = matcher.group(1);
            try {
                JSONArray jsonArray = JSON.parseArray(jsonArrayString);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    result.add(new DataDictItem(id, name));
                }
            } catch (Exception e) {
                logger.error("解析JSON数据时出错", e);
            }
        } else {
            logger.warn("未找到匹配的数据字典格式");
        }
        
        return result;
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

        boolean cacheEnabled = config.isCacheEnabled(); // 判断是否开启目录缓存

        if (cacheEnabled) {
            if (dataDictItemsCache != null) {
                logger.info("从缓存返回数据字典项列表");
                return new ArrayList<>(dataDictItemsCache); // 返回缓存以提高速度
            }
        }

        try {
            String currentAppCode = config.getDefaultAppCode();
            if (currentAppCode == null || currentAppCode.trim().isEmpty()) {
                logger.error("默认应用代码 (default-app-code) 未在配置文件中设置");
                return new ArrayList<>();
            }
            String url = buildUrl(currentAppCode);
            logger.info("下载数据字典，URL: {}", url);

            String jsContent = downloadWithProperEncoding(url);
            if (jsContent == null) {
                logger.error("无法下载数据字典JS文件");
                return new ArrayList<>();
            }

            List<DataDictItem> items = parseJsContent(jsContent);

            if (cacheEnabled) {
                synchronized (this) {
                    // 双重检查锁定，确保只初始化一次
                    if (dataDictItemsCache == null) {
                        this.dataDictItemsCache = new ArrayList<>(items); // 存储副本
                        logger.info("数据字典项列表已缓存");
                    }
                }
            }
            return items; // 返回新获取或已解析的列表
        } catch (Exception e) {
            logger.error("下载或解析数据字典时出错", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 构建数据字典详情URL
     */
    private String buildDetailUrl(String appCode, String classId) {
        return config.getBaseUrl() + String.format(DETAIL_API_TEMPLATE, appCode, classId);
    }
    
    /**
     * 使用正确编码下载内容
     */
    private String downloadWithProperEncoding(String url) {
        try {
            // 设置请求头，模拟浏览器请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Charset", "UTF-8,GBK;q=0.7,*;q=0.3");
            headers.set("Origin", "https://www.oyonyou.com");
            headers.set("Referer", "https://www.oyonyou.com/");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36");
            headers.set("Sec-Ch-Ua", "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"");
            headers.set("Sec-Ch-Ua-Mobile", "?0");
            headers.set("Sec-Ch-Ua-Platform", "\"Windows\"");
            headers.set("Sec-Fetch-Dest", "empty");
            headers.set("Sec-Fetch-Mode", "cors");
            headers.set("Sec-Fetch-Site", "same-site");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 直接使用String类型获取响应，RestTemplate会使用UTF-8编码处理
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String content = response.getBody();

            if (content != null) {
                logger.info("成功获取内容，长度: {}", content.length());
                return content;
            }

            return null;

        } catch (Exception e) {
            logger.error("下载内容时出错: {}", url, e);
            return null;
        }
    }

    /**
     * 检查编码是否有效（简单的启发式检查）
     */
    private boolean isValidEncoding(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查是否包含常见的乱码字符
        String[] invalidPatterns = {"�", "锟斤拷", "烫烫烫"};
        for (String pattern : invalidPatterns) {
            if (content.contains(pattern)) {
                return false;
            }
        }

        // 检查是否包含预期的JavaScript变量名
        if (content.contains("dataDictIndexData") || content.contains("fullClassname")) {
            return true;
        }

        // 基本的中文字符范围检查
        for (char c : content.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                return true; // 包含中文字符，认为编码正确
            }
        }

        return true; // 默认认为有效
    }

    /**
     * 解析数据字典详情JSON
     */
    private DataDictDetail parseDetailJson(String jsonContent, String classId) {
        try {
            JSONObject json = JSON.parseObject(jsonContent);

            DataDictDetail detail = new DataDictDetail();
            detail.setClassId(classId);
            detail.setFullClassName(json.getString("fullClassname"));
            detail.setDisplayName(json.getString("displayName"));
            detail.setDefaultTableName(json.getString("defaultTableName"));
            detail.setPrimary(json.getBooleanValue("isPrimary"));

            JSONArray propertyArray = json.getJSONArray("propertyVO");
            if (propertyArray != null) {
                List<DataDictDetail.Property> properties = new ArrayList<>();
                for (int i = 0; i < propertyArray.size(); i++) {
                    JSONObject propObj = propertyArray.getJSONObject(i);

                    DataDictDetail.Property property = new DataDictDetail.Property();
                    property.setName(propObj.getString("name"));
                    property.setDisplayName(propObj.getString("displayName"));
                    property.setDataTypeSql(propObj.getString("dataTypeSql"));
                    property.setKeyProp(propObj.getBooleanValue("keyProp"));
                    property.setNullable(propObj.getBooleanValue("nullable"));
                    property.setRefClassPathHref(propObj.getString("refClassPathHref"));

                    properties.add(property);
                }
                detail.setProperties(properties);
            }

            return detail;
        } catch (Exception e) {
            logger.error("解析数据字典详情JSON时出错", e);
            return null;
        }
    }
}