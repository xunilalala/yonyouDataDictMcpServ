package win.ixuni.yonyoudatadict.service;


import com.alibaba.fastjson.JSON;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.util.DataDictDownloader;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典服务类
 */
@Service
public class DataDictService {

    private final DataDictDownloader dataDictDownloader;

    @Autowired
    public DataDictService(DataDictDownloader dataDictDownloader) {
        this.dataDictDownloader = dataDictDownloader;
    }

    // ==================== 控制器专用方法 ====================
    
    /**
     * 下载默认应用代码下指定类ID的数据字典详情。
     *
     * @param classId 类ID
     * @return 数据字典详情的JSON字符串
     */
    @Tool(description = "根据类ID获取用友数据字典详情,你必须先通过searchDataDictItemsByName方法来获取准确的id,如果查询返回空，说明id错误，如果id正确，本方法一定返回数据,如果调用成功,你可以获得指定类的类名,主键，每个字段的名称,sql类型,引用的数据类型,是否为null,默认值等等，返回JSON格式的字符串",
            name = "getDataDictDetail"
    )
    public String downloadDataDictDetail(
            @ToolParam(description = "类ID") String classId
    ) {
        DataDictDetail detail = dataDictDownloader.downloadDataDictDetail(classId);
        return toOptimizedJsonString(detail);
    }

    /**
     * 下载默认应用代码下指定类ID的数据字典详情（控制器专用）
     *
     * @param classId 类ID
     * @return 数据字典详情
     */
    public DataDictDetail getDataDictDetailForController(String classId) {
        return dataDictDownloader.downloadDataDictDetail(classId);
    }

    /**
     * 下载默认应用代码的数据字典（控制器专用）
     *
     * @return 数据字典项列表
     */
    public List<DataDictItem> getDataDictItemsForController() {
        return dataDictDownloader.downloadDataDictItems();
    }

    // ==================== MCP工具方法 ====================

    /**
     * 根据名称模糊搜索默认应用代码下的用友数据字典条目。
     *
     * @param nameQuery 用于模糊搜索的名称查询字符串
     * @return 匹配的数据字典项列表的JSON字符串
     */
    @Tool(description = "根据名称模糊搜索用友数据字典条目及其类id，注意，nameQuery的值不易太长，虽然是模糊匹配，但是用的是like %nameQuery%的方式，你需要保证关键字不能太少(太少导致拆卸你的数据特别多，搜索麻烦)，查询的关键字不能太冗余(因为用的是包含式查询，并没有特别高的智能匹配,如果本方法返回空数组，可以尝试精简关键字或者换个关键字)，返回JSON格式的字符串",
            name = "searchDataDictItemsByName"
    )
    public String searchDataDictItemsByName(
            @ToolParam(description = "用于模糊搜索的名称查询字符串，仅允许是一个单词，比如:用户权限申请、org、流程生产订单、factory、人员等等,如果传入了多个单词，本方法将会放弃返回值，视为错误调用") String nameQuery
    ) {
        List<DataDictItem> allItems = dataDictDownloader.downloadDataDictItems();
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            return toOptimizedJsonString(allItems);
        }
        String lowerCaseQuery = nameQuery.toLowerCase();
        List<DataDictItem> filteredItems = allItems.stream()
                .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
        return toOptimizedJsonString(filteredItems);
    }

    /**
     * 根据名称模糊搜索默认应用代码下的用友数据字典条目（控制器专用）
     *
     * @param nameQuery 用于模糊搜索的名称查询字符串
     * @return 匹配的数据字典项列表
     */
    public List<DataDictItem> searchDataDictItemsByNameForController(String nameQuery) {
        List<DataDictItem> allItems = dataDictDownloader.downloadDataDictItems();
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            return allItems;
        }
        String lowerCaseQuery = nameQuery.toLowerCase();
        return allItems.stream()
                .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
    }

    /**
     * 优化的JSON序列化方法，减少转义字符
     */
    private String toOptimizedJsonString(Object obj) {
        return JSON.toJSONString(obj).replace("\\", "");
    }


}