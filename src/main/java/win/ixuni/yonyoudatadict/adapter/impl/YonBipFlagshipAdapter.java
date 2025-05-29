package win.ixuni.yonyoudatadict.adapter.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import win.ixuni.yonyoudatadict.adapter.VersionAdapter;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.model.YonyouVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YonBIP旗舰版适配器
 * 适配最新的YonBIP旗舰版数据格式
 * URL格式：https://media.oyonyou.com:18000/oyonyou/dict/{appCode}/scripts/data-dict-tree.js
 * 详情格式：https://media.oyonyou.com:18000/oyonyou/dict/{appCode}/dict/{classId}.json
 */
@Component
public class YonBipFlagshipAdapter implements VersionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(YonBipFlagshipAdapter.class);

    // 匹配旗舰版JS文件中的dataDictIndexData数组
    private static final Pattern DATA_PATTERN = Pattern.compile("var\\s+dataDictIndexData\\s*=\\s*(\\[.*?\\])\\s*;", Pattern.DOTALL);

    @Override
    public String buildDetailUrl(String baseUrl, String appCode, String classId) {
        // 旗舰版使用 /dict/{classId}.json 路径
        return baseUrl + "/" + appCode + "/dict/" + classId + ".json";
    }

    @Override
    public String buildDictListUrl(String baseUrl, String appCode) {
        // 旗舰版使用 /scripts/data-dict-tree.js 路径
        return baseUrl + "/" + appCode + "/scripts/data-dict-tree.js";
    }

    @Override
    public String getContentType() {
        return "application/javascript";
    }

    @Override
    public YonyouVersion getSupportedVersion() {
        return YonyouVersion.YONBIP_FLAGSHIP;
    }

    @Override
    public DataDictDetail parseDataDictDetail(String content, String classId) {
        try {
            JSONObject json = JSON.parseObject(content);

            DataDictDetail detail = new DataDictDetail();
            detail.setClassId(classId);

            // 旗舰版字段映射
            detail.setDisplayName(json.getString("displayName"));
            detail.setDefaultTableName(json.getString("tableName")); // 旗舰版使用tableName而不是defaultTableName
            detail.setFullClassName(json.getString("fullClassname")); // 兼容可能的字段名
            if (detail.getFullClassName() == null) {
                detail.setFullClassName(json.getString("className")); // 备用字段名
            }

            // 旗舰版使用primaryClass而不是isPrimary
            Boolean primaryClass = json.getBoolean("primaryClass");
            detail.setPrimary(primaryClass != null ? primaryClass : false);

            // 解析属性列表 - 旗舰版使用propertyVO
            JSONArray propertyArray = json.getJSONArray("propertyVO");
            if (propertyArray != null) {
                List<DataDictDetail.Property> properties = new ArrayList<>();

                for (int i = 0; i < propertyArray.size(); i++) {
                    JSONObject propObj = propertyArray.getJSONObject(i);

                    DataDictDetail.Property property = new DataDictDetail.Property();

                    // 旗舰版字段映射
                    property.setName(propObj.getString("name"));
                    property.setDisplayName(propObj.getString("displayName"));
                    property.setDataTypeSql(propObj.getString("dataTypeSql"));

                    // 布尔值处理
                    Boolean keyProp = propObj.getBoolean("keyProp");
                    property.setKeyProp(keyProp != null ? keyProp : false);

                    Boolean nullable = propObj.getBoolean("nullable");
                    property.setNullable(nullable != null ? nullable : true);

                    // 默认值
                    property.setDefaultValue(propObj.getString("defaultValue"));

                    // 旗舰版特有字段
                    property.setDataScope(propObj.getString("modelType")); // 使用modelType作为dataScope

                    // 引用类路径 - 旗舰版可能没有这个字段，设为null
                    property.setRefClassPathHref(propObj.getString("refClassPathHref"));

                    properties.add(property);
                }
                detail.setProperties(properties);
            }

            logger.info("YonBIP旗舰版详情解析成功，类名: {}, 表名: {}, 属性数量: {}",
                    detail.getDisplayName(), detail.getDefaultTableName(),
                    detail.getProperties() != null ? detail.getProperties().size() : 0);
            return detail;

        } catch (Exception e) {
            logger.error("解析YonBIP旗舰版数据字典详情JSON时出错", e);
            return null;
        }
    }

    @Override
    public List<DataDictItem> parseDataDictItems(String content) {
        List<DataDictItem> result = new ArrayList<>();

        Matcher matcher = DATA_PATTERN.matcher(content);
        if (matcher.find()) {
            String jsonArrayString = matcher.group(1);
            try {
                JSONArray jsonArray = JSON.parseArray(jsonArrayString);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    String id = obj.getString("id");
                    String name = obj.getString("name");

                    // 旗舰版特有字段：检查是否为DDC类（isDdcClass为true的才是真正的数据字典类）
                    Boolean isDdcClass = obj.getBoolean("isDdcClass");
                    String pId = obj.getString("pId");

                    // 只添加有效的数据字典项
                    // 1. 必须有id和name
                    // 2. 对于旗舰版，优先选择isDdcClass为true的项，但也包含一些重要的父节点
                    if (id != null && name != null && !id.isEmpty() && !name.isEmpty()) {
                        // 过滤掉一些明显的分类节点，保留实际的数据字典项
                        if (shouldIncludeItem(id, name, isDdcClass, pId)) {
                            result.add(new DataDictItem(id, name));
                        }
                    }
                }

                logger.info("YonBIP旗舰版解析成功，共获取{}个数据字典项", result.size());
            } catch (Exception e) {
                logger.error("解析YonBIP旗舰版JSON数据时出错", e);
            }
        } else {
            logger.warn("未找到YonBIP旗舰版匹配的dataDictIndexData格式");
        }

        return result;
    }

    /**
     * 判断是否应该包含此数据字典项
     * 旗舰版的数据结构包含很多分类节点，需要过滤
     */
    private boolean shouldIncludeItem(String id, String name, Boolean isDdcClass, String pId) {
        // 如果明确标记为DDC类，直接包含
        if (isDdcClass != null && isDdcClass) {
            return true;
        }

        // 过滤掉明显的分类节点
        if (id.startsWith("md_") && id.contains("_") && id.length() < 20) {
            // md_clazz, md__iuap 等是分类节点，不包含
            return false;
        }

        if (id.startsWith("db_") && id.contains("_") && id.length() < 20) {
            // db_table, db__mm 等是分类节点，不包含
            return false;
        }

        // 包含数字ID的项通常是真正的数据字典项（如：380714163413975040）
        if (id.matches("\\d+")) {
            return true;
        }

        // 其他情况下，如果name包含表明这是一个具体实体的关键词，也包含
        String lowerName = name.toLowerCase();
        if (lowerName.contains("表") || lowerName.contains("table") ||
                lowerName.contains("entity") || lowerName.contains("class")) {
            return true;
        }

        // 默认不包含
        return false;
    }

}