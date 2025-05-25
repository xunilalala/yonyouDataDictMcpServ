package win.ixuni.yonyoudatadict.processor;


import win.ixuni.yonyoudatadict.model.DataDictDetail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefClassPathHrefProcessor implements DataDictProcessor {

    // 正则表达式用于匹配 onClick=loadDataDict("ID");>名称</a> 格式的字符串
    // 例如: <a href="javascript:void(0);" onClick=loadDataDict("1451v9ziqm");>组织</a> (org)
    // 捕获组1: ID (例如 "1451v9ziqm")
    // 捕获组2: 名称 (例如 "组织")
    private static final Pattern REF_CLASS_PATH_PATTERN = Pattern.compile("onClick=loadDataDict\\(\"([^\"]+)\"\\);>([^<]+)</a>");

    @Override
    public DataDictDetail process(DataDictDetail detail) {
        if (detail == null || detail.getProperties() == null) {
            return detail;
        }

        for (DataDictDetail.Property property : detail.getProperties()) {
            String refClassPathHref = property.getRefClassPathHref();
            if (refClassPathHref != null && !refClassPathHref.isEmpty()) {
                Matcher matcher = REF_CLASS_PATH_PATTERN.matcher(refClassPathHref);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    String name = matcher.group(2).trim(); // 提取并去除名称两边的空格
                    property.setRefClassPathHref("引用的类id:" + id + ";名称:" + name);
                }
            }
        }
        return detail;
    }

}
