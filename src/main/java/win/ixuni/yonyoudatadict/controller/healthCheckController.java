package win.ixuni.yonyoudatadict.controller;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.service.DataDictService;

import java.util.List;

@RestController
@RequestMapping("/check")
public class healthCheckController {

    private final DataDictService dataDictService;

    public healthCheckController(DataDictService dataDictService) {
        this.dataDictService = dataDictService;
    }

    @RequestMapping()
    public String healthCheck() {
        return "OK";
    }


    // 测试工具方法：根据类ID获取数据字典详情
    @RequestMapping("/tool/detail/{classId}")
    public DataDictDetail testToolGetDetail(
            @PathVariable("classId") String classId) {
        return dataDictService.downloadDataDictDetail(classId);
    }

    // 测试工具方法：根据名称搜索数据字典条目
    @RequestMapping("/tool/search")
    public List<DataDictItem> testToolSearch(
            @RequestParam(value = "name", required = false, defaultValue = "") String nameQuery) {
        return dataDictService.searchDataDictItemsByName(nameQuery);
    }

}
