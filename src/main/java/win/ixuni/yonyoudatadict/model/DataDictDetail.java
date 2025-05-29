package win.ixuni.yonyoudatadict.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据字典详情模型
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataDictDetail {

    private String classId;

    private String fullClassName;

    private String displayName;

    private String defaultTableName;

    private boolean isPrimary;

    private List<Property> properties;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Property {

        private String name;

        private String displayName;

        private String dataTypeSql;

        private boolean keyProp;

        private boolean nullable;

        private String refClassPathHref;

        // 新增字段：默认值
        private String defaultValue;

        // 新增字段：数据范围说明
        private String dataScope;

        // 新增字段：字段编码
        private String columnName;

        // 新增字段：引用模型名称
        private String refModelName;

        // 新增字段：引用类ID
        private String refClass;

        // 新增字段：枚举值
        private String enumValues;

        // 新增字段：是否主键
        private boolean primaryKey;

    }

}
