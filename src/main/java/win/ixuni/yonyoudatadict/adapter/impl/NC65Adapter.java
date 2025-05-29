package win.ixuni.yonyoudatadict.adapter.impl;


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
 * NC65版本适配器
 * 针对 https://www.oyonyou.com/dict/{appcode}/static/js/data-dict-tree.js 格式
 * 列表页面：JS格式 dataDictIndexData
 * 详情页面：HTML格式 /ddc/{id}.html
 */
@Component
public class NC65Adapter implements VersionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NC65Adapter.class);

    // JS数据提取的正则表达式
    private static final Pattern DATA_DICT_PATTERN = Pattern.compile(
            "var\\s+dataDictIndexData\\s*=\\s*(\\[.*?\\]);?",
            Pattern.DOTALL
    );

    // 单个数据项的正则表达式
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "\\{\\s*id:\\s*[\"']?(\\d+)[\"']?\\s*,\\s*name:\\s*[\"']([^\"']+)[\"']\\s*\\}",
            Pattern.DOTALL
    );

    @Override
    public String buildDetailUrl(String baseUrl, String appCode, String classId) {
        // 用友之家的详情页面格式：/dict/{appcode}/ddc/{id}.html
        return "https://www.oyonyou.com/dict/" + appCode + "/ddc/" + classId + ".html";
    }

    @Override
    public String buildDictListUrl(String baseUrl, String appCode) {
        // 用友之家的字典列表格式：/dict/{appcode}/static/js/data-dict-tree.js
        return "https://www.oyonyou.com/dict/" + appCode + "/static/js/data-dict-tree.js";
    }

    @Override
    public String getContentType() {
        return "application/javascript"; // 列表页面是JS格式
    }

    @Override
    public Map<String, String> getSpecialHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/javascript, text/javascript, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.put("Referer", "https://www.oyonyou.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        return headers;
    }

    @Override
    public YonyouVersion getSupportedVersion() {
        return YonyouVersion.NC65_OYONYOU;
    }

    @Override
    public boolean needsSpecialHeaders() {
        return true;
    }

    @Override
    public DataDictDetail parseDataDictDetail(String content, String classId) {
        try {
            Document doc = Jsoup.parse(content);

            DataDictDetail detail = new DataDictDetail();
            detail.setClassId(classId);

            // 解析标题信息
            Element titleDiv = doc.select("div.title").first();
            if (titleDiv != null) {
                Elements titleSpans = titleDiv.select("span");
                if (titleSpans.size() >= 2) {
                    // 第一个span是表名
                    String displayName = titleSpans.get(0).text().trim();
                    detail.setDisplayName(displayName);

                    // 第二个span包含表名和VO类名
                    String fullInfo = titleSpans.get(1).text().trim();
                    // 格式：(cp_appscategory / nc.uap.cpb.org.vos.CpAppsCategoryVO)
                    if (fullInfo.contains("/")) {
                        String[] parts = fullInfo.split("/");
                        if (parts.length >= 2) {
                            String tableName = parts[0].trim().replaceAll("[()]", "");
                            String className = parts[1].trim().replaceAll("[()]", "");
                            detail.setDefaultTableName(tableName);
                            detail.setFullClassName(className);
                        }
                    }
                }
            }

            // 解析属性表格
            List<DataDictDetail.Property> properties = new ArrayList<>();
            Element propTable = doc.select("table#propTable").first();

            if (propTable != null) {
                Elements rows = propTable.select("tr");

                // 跳过表头行
                for (int i = 1; i < rows.size(); i++) {
                    Element row = rows.get(i);
                    Elements cells = row.select("td");

                    if (cells.size() >= 9) {
                        DataDictDetail.Property property = new DataDictDetail.Property();

                        // 序号在第0列，跳过
                        property.setName(cells.get(1).text().trim());              // 属性编码
                        property.setDisplayName(cells.get(2).text().trim());       // 属性名称
                        property.setColumnName(cells.get(3).text().trim());        // 字段编码
                        property.setDataTypeSql(cells.get(4).text().trim());       // 字段类型

                        // 是否必输 (第5列)
                        String requiredText = cells.get(5).text().trim();
                        property.setNullable(!"√".equals(requiredText));

                        // 引用模型 (第6列)
                        Element refElement = cells.get(6);
                        String refModel = refElement.text().trim();
                        property.setRefModelName(refModel);

                        // 检查是否有链接到其他类
                        Element refLink = refElement.select("a").first();
                        if (refLink != null) {
                            String href = refLink.attr("href");
                            // 从href中提取引用的类ID
                            if (href.contains("./") && href.endsWith(".html")) {
                                String refClassId = href.replace("./", "").replace(".html", "");
                                property.setRefClass(refClassId);
                            }
                        }

                        // 默认值 (第7列)
                        property.setDefaultValue(cells.get(7).text().trim());

                        // 取值范围/枚举 (第8列)
                        String enumValues = cells.get(8).text().trim();
                        if (!enumValues.isEmpty()) {
                            property.setEnumValues(enumValues);
                        }

                        // 检查是否为主键
                        if (row.hasClass("pk-row")) {
                            property.setPrimaryKey(true);
                        }

                        properties.add(property);
                    }
                }
            }

            detail.setProperties(properties);

            logger.info("NC65详情解析成功，类名: {}, 表名: {}, 属性数量: {}",
                    detail.getDisplayName(), detail.getDefaultTableName(), properties.size());

            return detail;

        } catch (Exception e) {
            logger.error("解析NC65数据字典详情HTML时出错", e);
            return null;
        }
    }

    @Override
    public List<DataDictItem> parseDataDictItems(String content) {
        List<DataDictItem> result = new ArrayList<>();

        try {
            // 提取 dataDictIndexData 数组
            Matcher dataMatcher = DATA_DICT_PATTERN.matcher(content);
            if (!dataMatcher.find()) {
                logger.warn("未找到 dataDictIndexData 数据");
                return result;
            }

            String dataArray = dataMatcher.group(1);
            logger.debug("提取到的数据数组: {}", dataArray);

            // 解析每个数据项
            Matcher itemMatcher = ITEM_PATTERN.matcher(dataArray);
            while (itemMatcher.find()) {
                String id = itemMatcher.group(1);
                String name = itemMatcher.group(2);

                // 清理名称中的多余空格和特殊字符
                name = name.trim().replaceAll("\\s+", " ");

                result.add(new DataDictItem(id, name));
                logger.debug("解析到数据项: id={}, name={}", id, name);
            }

            logger.info("NC65解析成功，共获取{}个数据字典项", result.size());

        } catch (Exception e) {
            logger.error("解析NC65 JS数据时出错", e);
        }

        return result;
    }

}