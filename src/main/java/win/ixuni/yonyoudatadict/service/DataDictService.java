package win.ixuni.yonyoudatadict.service;


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

    /**
     * 下载默认应用代码的数据字典。
     *
     * @return 数据字典项列表
     */
    public List<DataDictItem> downloadDataDict() {
        return dataDictDownloader.downloadDataDictItems();
    }

    /**
     * 下载默认应用代码下指定类ID的数据字典详情。
     *
     * @param classId 类ID
     * @return 数据字典详情
     */
    @Tool(description = "根据类ID获取用友数据字典详情",
            name = "getDataDictDetail"
    )
    public DataDictDetail downloadDataDictDetail(
            @ToolParam(description = "类ID") String classId
    ) {
        return dataDictDownloader.downloadDataDictDetail(classId);
    }


    /**
     * 根据名称模糊搜索默认应用代码下的用友数据字典条目。
     *
     * @param nameQuery 用于模糊搜索的名称查询字符串
     * @return 匹配的数据字典项列表
     */
    @Tool(description = "根据名称模糊搜索用友数据字典条目及其类id",
            name = "searchDataDictItemsByName"
    )
    public List<DataDictItem> searchDataDictItemsByName(
            @ToolParam(description = "用于模糊搜索的名称查询字符串") String nameQuery
    ) {
        List<DataDictItem> allItems = dataDictDownloader.downloadDataDictItems();
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            return allItems;
        }
        String lowerCaseQuery = nameQuery.toLowerCase();
        return allItems.stream()
                .filter(item -> item.getName() != null && item.getName().toLowerCase().contains(lowerCaseQuery))
                .collect(Collectors.toList());
    }

}