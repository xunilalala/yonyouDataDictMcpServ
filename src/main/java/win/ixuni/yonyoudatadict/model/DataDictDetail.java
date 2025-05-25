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

    }

}
