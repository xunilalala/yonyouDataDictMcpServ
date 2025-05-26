package win.ixuni.yonyoudatadict.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.util.DataDictDownloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefClassPathHrefProcessor implements DataDictProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RefClassPathHrefProcessor.class);

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
                    String fullClassName = getFullClassNameById(id);
                    property.setRefClassPathHref("引用的类id:" + id + ";全类名:" + fullClassName);
                }
            }
        }
        return detail;
    }

    /**
     * 根据类ID获取全类名
     */
    private String getFullClassNameById(String classId) {
        try {
            DataDictDownloader downloader = DataDictDownloader.getInstance();
            if (downloader != null) {
                // 使用 false 参数避免应用处理器链，防止死循环
                DataDictDetail refDetail = downloader.downloadDataDictDetail(classId, false);
                if (refDetail != null && refDetail.getFullClassName() != null) {
                    return refDetail.getFullClassName();
                }
            }
        } catch (Exception e) {
            logger.warn("获取类ID为 {} 的全类名时出错: {}", classId, e.getMessage());
        }
        return "未知";
    }
}
