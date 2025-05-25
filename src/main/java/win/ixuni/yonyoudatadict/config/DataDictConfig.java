package win.ixuni.yonyoudatadict.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 数据字典配置类
 */
@Configuration
@ConfigurationProperties(prefix = "data-dict")
@Data
public class DataDictConfig {

    private String baseUrl;

    private String staticPath;

    private String defaultAppCode;

    private boolean cacheEnabled;

    private int cacheSize = 100; // 默认缓存大小

    private CustomFieldRemoval customFieldRemoval = new CustomFieldRemoval(); // 新增自定义字段移除配置

    @Data
    public static class CustomFieldRemoval {

        private boolean enabled = false; // 默认禁用

    }
}