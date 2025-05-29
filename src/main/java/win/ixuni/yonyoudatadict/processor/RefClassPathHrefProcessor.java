package win.ixuni.yonyoudatadict.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.YonyouVersion;
import win.ixuni.yonyoudatadict.util.DataDictDownloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 引用类路径处理器 - 版本感知
 */
public class RefClassPathHrefProcessor extends VersionAwareProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RefClassPathHrefProcessor.class);

    // 高级版：正则表达式用于匹配 onClick=loadDataDict("ID");>名称</a> 格式的字符串
    private static final Pattern REF_CLASS_PATH_PATTERN = Pattern.compile("onClick=loadDataDict\\(\"([^\"]+)\"\\);>([^<]+)</a>");

    @Override
    protected DataDictDetail processForVersion(DataDictDetail detail, YonyouVersion version) {
        if (detail == null || detail.getProperties() == null) {
            return detail;
        }

        switch (version) {
            case YONBIP_ADVANCED:
                return processAdvancedVersion(detail);
            case YONBIP_FLAGSHIP:
                return processFlagshipVersion(detail);
            case NC65_OYONYOU:
                return processNC65Version(detail);
            case NCCLOUD:
                return processNCCloudVersion(detail);
            default:
                // 默认使用高级版处理
                return processAdvancedVersion(detail);
        }
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
                if (refDetail != null) {
                    String fullClassName = refDetail.getFullClassName();
                    if (fullClassName != null && !fullClassName.isEmpty()) {
                        return fullClassName;
                    }
                    // 如果没有fullClassName，使用displayName
                    String displayName = refDetail.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取类ID为 {} 的全类名时出错: {}", classId, e.getMessage());
        }
        return "未知";
    }

    /**
     * 处理YonBIP高级版的引用类路径
     */
    private DataDictDetail processAdvancedVersion(DataDictDetail detail) {
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
     * 处理YonBIP旗舰版的引用类路径
     * 旗舰版可能没有refClassPathHref字段，或者格式不同
     */
    private DataDictDetail processFlagshipVersion(DataDictDetail detail) {
        for (DataDictDetail.Property property : detail.getProperties()) {
            String refClassPathHref = property.getRefClassPathHref();

            // 旗舰版可能使用不同的字段或格式
            if (refClassPathHref != null && !refClassPathHref.isEmpty()) {
                // 如果是数字ID格式（旗舰版常用格式）
                if (refClassPathHref.matches("\\d+")) {
                    String fullClassName = getFullClassNameById(refClassPathHref);
                    property.setRefClassPathHref("引用的类id:" + refClassPathHref + ";全类名:" + fullClassName);
                } else {
                    // 尝试高级版的解析方式
                    Matcher matcher = REF_CLASS_PATH_PATTERN.matcher(refClassPathHref);
                    if (matcher.find()) {
                        String id = matcher.group(1);
                        String fullClassName = getFullClassNameById(id);
                        property.setRefClassPathHref("引用的类id:" + id + ";全类名:" + fullClassName);
                    }
                }
            }

            // 旗舰版特殊处理：检查dataScope字段中的模块信息
            String dataScope = property.getDataScope();
            if (dataScope != null && !dataScope.isEmpty() && !"md".equals(dataScope)) {
                // 如果dataScope不是md（元数据），可能包含有用的引用信息
                property.setDataScope(dataScope + " (模块类型)");
            }
        }
        return detail;
    }

    /**
     * 处理NC65版本的引用类路径
     */
    private DataDictDetail processNC65Version(DataDictDetail detail) {
        // NC65版本可能有不同的引用格式，暂时使用默认处理
        return processAdvancedVersion(detail);
    }

    /**
     * 处理NCCloud版本的引用类路径
     */
    private DataDictDetail processNCCloudVersion(DataDictDetail detail) {
        for (DataDictDetail.Property property : detail.getProperties()) {
            String refClassPathHref = property.getRefClassPathHref();

            // NCCloud的引用模型格式：单据状态 (BillstatusEnum)
            if (refClassPathHref != null && !refClassPathHref.isEmpty()) {
                // 如果包含括号，提取括号内的内容作为引用类型
                if (refClassPathHref.contains("(") && refClassPathHref.contains(")")) {
                    String refType = refClassPathHref.substring(
                            refClassPathHref.indexOf("(") + 1,
                            refClassPathHref.lastIndexOf(")")
                    ).trim();
                    String description = refClassPathHref.substring(0, refClassPathHref.indexOf("(")).trim();
                    property.setRefClassPathHref("引用类型:" + refType + ";描述:" + description);
                }
            }

            // NCCloud特殊处理：处理枚举值信息
            String dataScope = property.getDataScope();
            if (dataScope != null && dataScope.startsWith("枚举值:")) {
                // 保持枚举值信息不变，这是NCCloud的特色功能
                // 枚举值已经在适配器中处理好了
            }
        }
        return detail;
    }
}
