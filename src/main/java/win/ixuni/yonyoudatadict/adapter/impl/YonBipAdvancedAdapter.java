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
 * YonBIP高级版适配器
 * 解析JS文件中的dataDictIndexData数组
 */
@Component
public class YonBipAdvancedAdapter implements VersionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(YonBipAdvancedAdapter.class);

    // 用于提取JS中的JSON数组的正则表达式
    private static final Pattern DATA_PATTERN = Pattern.compile("var\\s+dataDictIndexData\\s*=\\s*(\\[.*?\\])\\s*;", Pattern.DOTALL);

    // 数据字典详情API路径模板
    private static final String DETAIL_API_TEMPLATE = "/%s/dict/%s.json";

    @Override
    public String buildDetailUrl(String baseUrl, String appCode, String classId) {
        return baseUrl + String.format(DETAIL_API_TEMPLATE, appCode, classId);
    }

    @Override
    public String buildDictListUrl(String baseUrl, String appCode) {
        return baseUrl + "/" + appCode + "/static/js/data-dict-tree.js";
    }

    @Override
    public String getContentType() {
        return "application/javascript";
    }

    @Override
    public YonyouVersion getSupportedVersion() {
        return YonyouVersion.YONBIP_ADVANCED;
    }

    @Override
    public DataDictDetail parseDataDictDetail(String content, String classId) {
        try {
            JSONObject json = JSON.parseObject(content);

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
                    property.setDefaultValue(propObj.getString("defaultValue"));
                    property.setDataScope(propObj.getString("dataScope"));

                    properties.add(property);
                }
                detail.setProperties(properties);
            }

            logger.info("YonBIP高级版详情解析成功，类名: {}", detail.getDisplayName());
            return detail;
        } catch (Exception e) {
            logger.error("解析YonBIP高级版数据字典详情JSON时出错", e);
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
                    result.add(new DataDictItem(id, name));
                }
                logger.info("YonBIP高级版解析成功，共获取{}个数据字典项", result.size());
            } catch (Exception e) {
                logger.error("解析YonBIP高级版JSON数据时出错", e);
            }
        } else {
            logger.warn("未找到YonBIP高级版匹配的数据字典格式");
        }

        return result;
    }

}