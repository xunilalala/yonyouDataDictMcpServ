package win.ixuni.yonyoudatadict.adapter.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import win.ixuni.yonyoudatadict.adapter.VersionAdapter;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.model.YonyouVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NCCloud适配器
 * 适配NCCloud版本数据格式
 * URL格式：https://www.oyonyou.com/dict/{appCode}/static/js/data-dict-tree.js
 * 详情格式：https://www.oyonyou.com/dict/{appCode}/ddc/{classId}.html
 */
@Component
public class NCCloudAdapter implements VersionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NCCloudAdapter.class);

    // 匹配NCCloud JS文件中的dataDictIndexData数组
    private static final Pattern DATA_PATTERN = Pattern.compile("var\\s+dataDictIndexData\\s*=\\s*(\\[.*?\\])\\s*;", Pattern.DOTALL);

    @Override
    public String buildDetailUrl(String baseUrl, String appCode, String classId) {
        // NCCloud使用 /ddc/{classId}.html 路径
        return "https://www.oyonyou.com/dict/" + appCode + "/ddc/" + classId + ".html";
    }

    @Override
    public String buildDictListUrl(String baseUrl, String appCode) {
        // NCCloud使用 /static/js/data-dict-tree.js 路径
        return "https://www.oyonyou.com/dict/" + appCode + "/static/js/data-dict-tree.js";
    }

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public Map<String, String> getSpecialHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Cache-Control", "no-cache");
        return headers;
    }

    @Override
    public YonyouVersion getSupportedVersion() {
        return YonyouVersion.NCCLOUD;
    }

    @Override
    public boolean needsSpecialHeaders() {
        return true;
    }

    @Override
    public DataDictDetail parseDataDictDetail(String content, String classId) {
        try {
            // 使用Jsoup解析HTML
            Document doc = Jsoup.parse(content);

            DataDictDetail detail = new DataDictDetail();
            detail.setClassId(classId);

            // 解析标题信息
            Element titleElement = doc.select("div.title span").first();
            if (titleElement != null) {
                String titleText = titleElement.text();
                // 提取显示名称和类信息
                // 格式：销户申请主表 (tam_applybill / nc.vo.tam.account.destroy.DestroyApplyVO)
                if (titleText.contains("(") && titleText.contains(")")) {
                    String displayName = titleText.substring(0, titleText.indexOf("(")).trim();
                    String classInfo = titleText.substring(titleText.indexOf("(") + 1, titleText.lastIndexOf(")"));

                    detail.setDisplayName(displayName);

                    // 解析表名和类名
                    if (classInfo.contains("/")) {
                        String[] parts = classInfo.split("/");
                        if (parts.length >= 2) {
                            detail.setDefaultTableName(parts[0].trim());
                            detail.setFullClassName(parts[1].trim());
                        }
                    }
                } else {
                    detail.setDisplayName(titleText);
                }
            }

            // 解析属性表格
            Element propTable = doc.getElementById("propTable");
            if (propTable != null) {
                Elements rows = propTable.select("tr");
                List<DataDictDetail.Property> properties = new ArrayList<>();

                // 跳过表头（第一行）
                for (int i = 1; i < rows.size(); i++) {
                    Element row = rows.get(i);
                    Elements cells = row.select("td");

                    if (cells.size() >= 9) { // NCCloud表格有9列
                        DataDictDetail.Property property = new DataDictDetail.Property();

                        // 解析各列数据
                        // 序号 | 属性编码 | 属性名称 | 字段编码 | 字段类型 | 是否必输 | 引用模型 | 默认值 | 取值范围/枚举
                        property.setName(cells.get(1).text().trim()); // 属性编码
                        property.setDisplayName(cells.get(2).text().trim()); // 属性名称
                        property.setDataTypeSql(cells.get(4).text().trim()); // 字段类型

                        // 是否必输
                        String required = cells.get(5).text().trim();
                        property.setNullable(!"√".equals(required)); // √表示必输，即不可为空

                        // 引用模型
                        String refModel = cells.get(6).text().trim();
                        if (!refModel.isEmpty()) {
                            property.setRefClassPathHref(refModel);
                        }

                        // 默认值
                        String defaultValue = cells.get(7).text().trim();
                        if (!defaultValue.isEmpty()) {
                            property.setDefaultValue(defaultValue);
                        }

                        // 取值范围/枚举
                        String enumValues = cells.get(8).html(); // 使用html()获取包含<br>的内容
                        if (!enumValues.isEmpty()) {
                            // 处理枚举值，将<br>替换为换行符
                            enumValues = enumValues.replaceAll("<br\\s*/?>", "\n");
                            enumValues = Jsoup.parse(enumValues).text(); // 去除HTML标签
                            if (!enumValues.trim().isEmpty()) {
                                property.setDataScope("枚举值: " + enumValues.trim());
                            }
                        }

                        // 检查是否为主键
                        boolean isPrimaryKey = row.hasClass("pk-row") ||
                                property.getName().toLowerCase().contains("pk_") ||
                                (refModel.contains("主键") || refModel.contains("UFID"));
                        property.setKeyProp(isPrimaryKey);

                        properties.add(property);
                    }
                }
                detail.setProperties(properties);
            }

            // NCCloud默认不是主要类（根据具体需求可以调整）
            detail.setPrimary(false);

            logger.info("NCCloud详情解析成功，类名: {}, 表名: {}, 属性数量: {}",
                    detail.getDisplayName(), detail.getDefaultTableName(),
                    detail.getProperties() != null ? detail.getProperties().size() : 0);
            return detail;

        } catch (Exception e) {
            logger.error("解析NCCloud数据字典详情HTML时出错", e);
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

                    // NCCloud的数据结构比较简单，只有id和name
                    if (id != null && name != null && !id.isEmpty() && !name.isEmpty()) {
                        result.add(new DataDictItem(id, name));
                    }
                }

                logger.info("NCCloud解析成功，共获取{}个数据字典项", result.size());
            } catch (Exception e) {
                logger.error("解析NCCloud JSON数据时出错", e);
            }
        } else {
            logger.warn("未找到NCCloud匹配的dataDictIndexData格式");
        }

        return result;
    }

}