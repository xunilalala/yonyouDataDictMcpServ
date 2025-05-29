package win.ixuni.yonyoudatadict.adapter;


import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.model.YonyouVersion;

import java.util.List;

/**
 * 用友版本适配器接口
 * 每个版本需要实现此接口来处理特定的数据解析逻辑
 */
public interface VersionAdapter {

    /**
     * 构建数据字典详情的完整URL
     */
    String buildDetailUrl(String baseUrl, String appCode, String classId);

    /**
     * 构建数据字典列表的完整URL
     */
    String buildDictListUrl(String baseUrl, String appCode);

    /**
     * 获取请求内容类型（JS、HTML、JSON等）
     */
    String getContentType();

    /**
     * 获取特殊请求头
     */
    default java.util.Map<String, String> getSpecialHeaders() {
        return java.util.Collections.emptyMap();
    }

    /**
     * 获取支持的版本
     */
    YonyouVersion getSupportedVersion();

    /**
     * 是否需要特殊的请求头
     */
    default boolean needsSpecialHeaders() {
        return false;
    }

    /**
     * 解析数据字典详情内容
     */
    DataDictDetail parseDataDictDetail(String content, String classId);

    /**
     * 解析数据字典列表内容
     */
    List<DataDictItem> parseDataDictItems(String content);

}