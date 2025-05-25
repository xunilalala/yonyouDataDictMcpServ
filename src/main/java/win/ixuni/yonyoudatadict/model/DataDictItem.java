package win.ixuni.yonyoudatadict.model;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 数据字典项模型类
 */
@Data
@AllArgsConstructor
public class DataDictItem {
    private String id;
    private String name;
}