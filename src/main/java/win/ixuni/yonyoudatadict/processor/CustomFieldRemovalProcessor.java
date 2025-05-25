package win.ixuni.yonyoudatadict.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.ixuni.yonyoudatadict.config.DataDictConfig;
import win.ixuni.yonyoudatadict.model.DataDictDetail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomFieldRemovalProcessor implements DataDictProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CustomFieldRemovalProcessor.class);

    // 正则表达式匹配以任意字母开头（可选），后跟 "def" 和一个或多个数字的名称
    // 或者以 "vfree" 开头，后跟一个或多个数字的名称
    // 例如: "def1", "vdef10", "customdef123", "vfree1", "vfree10"
    private static final Pattern CUSTOM_FIELD_PATTERN = Pattern.compile("^(?:[a-zA-Z]*def\\d+|vfree\\d+)$");

    private final DataDictConfig config;

    public CustomFieldRemovalProcessor(DataDictConfig config) {
        this.config = config;
    }

    @Override
    public DataDictDetail process(DataDictDetail detail) {
        if (config.getCustomFieldRemoval() == null || !config.getCustomFieldRemoval().isEnabled()) {
            return detail; // 如果配置不存在或未启用，则不执行任何操作
        }

        if (detail == null || detail.getProperties() == null || detail.getProperties().isEmpty()) {
            return detail;
        }

        List<String> removedFieldNames = new ArrayList<>();
        Iterator<DataDictDetail.Property> iterator = detail.getProperties().iterator();

        while (iterator.hasNext()) {
            DataDictDetail.Property property = iterator.next();
            if (property.getName() != null) {
                Matcher matcher = CUSTOM_FIELD_PATTERN.matcher(property.getName());
                if (matcher.matches()) {
                    removedFieldNames.add(property.getName() + " (" + property.getDisplayName() + ")");
                    iterator.remove();
                }
            }
        }

        if (!removedFieldNames.isEmpty()) {
            logger.info("在数据字典 '{}' (Class ID: {}) 中移除了以下自定义字段: {}",
                    detail.getDisplayName(), detail.getClassId(), String.join(", ", removedFieldNames));
        }

        return detail;
    }

}
