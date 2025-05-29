package win.ixuni.yonyoudatadict.model;


import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 用友版本枚举
 * 支持多个应用代码映射到同一个版本策略
 */
@Getter
public enum YonyouVersion {

    /**
     * YonBIP 高级版
     * 特点：解析JS文件，数据格式为 dataDictIndexData
     */
    YONBIP_ADVANCED("yonbip-advanced", "YonBIP高级版", "JS",
            Arrays.asList("yonbip3ddc", "yonbip3ddcjianzhu", "yonbip3ddcsp1", "yonbip3ddcr5")),

    /**
     * YonBIP 旗舰版
     * 特点：解析JS文件，但数据格式与高级版不同
     * 使用 /scripts/data-dict-tree.js 路径
     * 支持多个小版本：r5bip2, r6bip2, r7bip2 等
     */
    YONBIP_FLAGSHIP("yonbip-flagship", "YonBIP旗舰版", "JS",
            Arrays.asList("yonbip3r5bip2", "yonbip3r6bip2", "yonbip3r8bip2", "yonbip3r5bip", "yonbip3r6bip")),

    /**
     * NC65
     * 特点：使用 oyonyou.com 域名，JS格式列表+HTML详情页面
     */
    NC65_OYONYOU("ncddc0065", "NC65版", "JS",
            Arrays.asList("ncddc0065")),

    /**
     * NCCLOUD
     * 特点：解析JS文件和HTML详情
     */
    NCCLOUD("nccloud", "NCCloud", "HYBRID",
            Arrays.asList("nccddc1909", "nccddc2005", "nccddc2105", "nccloud",
                    "nccddc1811", "nccddc1903",
                    "nccddc2105yiliao", "nccddc2105jianzhu",
                    "nccddc2111", "nccddc2111jianzhu")),

    /**
     * 未知版本
     */
    UNKNOWN("unknown", "未知版本", "UNKNOWN",
            Arrays.asList());

    private final String code;

    private final String displayName;

    private final String parseType;

    private final List<String> supportedAppCodes; // 支持的应用代码列表

    YonyouVersion(String code, String displayName, String parseType, List<String> supportedAppCodes) {
        this.code = code;
        this.displayName = displayName;
        this.parseType = parseType;
        this.supportedAppCodes = supportedAppCodes;
    }

    /**
     * 添加新的应用代码支持（运行时动态添加）
     */
    public static void addAppCodeSupport(YonyouVersion version, String appCode) {
        if (version != null && appCode != null && !appCode.trim().isEmpty()) {
            version.supportedAppCodes.add(appCode.toLowerCase().trim());
        }
    }

    /**
     * 根据应用代码判断版本（支持多对一映射）
     */
    public static YonyouVersion fromAppCode(String appCode) {
        if (appCode == null || appCode.trim().isEmpty()) {
            return UNKNOWN;
        }

        String lowerCode = appCode.toLowerCase();

        // 首先检查精确匹配
        for (YonyouVersion version : values()) {
            if (version.supportedAppCodes.contains(lowerCode)) {
                return version;
            }
        }

        // 如果没有精确匹配，使用模式匹配
        return fromAppCodeByPattern(lowerCode);
    }

    /**
     * 通过模式匹配确定版本（兜底策略）
     */
    private static YonyouVersion fromAppCodeByPattern(String lowerCode) {
        // NC65 用友之家版本模式匹配
        // 格式如：ncddc0065
        if (lowerCode.matches("ncddc\\d+.*")) {
            return NC65_OYONYOU;
        }

        // YonBIP旗舰版模式匹配（支持未来的新版本号）
        // 格式如：yonbip3r5bip2, yonbip3r6bip2, yonbip3r9bip3 等
        if (lowerCode.matches("yonbip\\d+r\\d+bip\\d+.*")) {
            return YONBIP_FLAGSHIP;
        }

        // NCCloud模式匹配（支持未来的新版本）
        // 格式如：nccddc1909, nccddc2105 等
        if (lowerCode.matches("ncc?ddc\\d+.*")) {
            return NCCLOUD;
        }

        // YonBIP高级版模式匹配
        if (lowerCode.contains("yonbip") && lowerCode.contains("ddc")) {
            return YONBIP_ADVANCED;
        }

        // NC65模式匹配
        if (lowerCode.contains("nc") && (lowerCode.contains("65") || lowerCode.contains("0065"))) {
            return NC65_OYONYOU;
        }

        // 通用NCCloud匹配
        if (lowerCode.contains("cloud")) {
            return NCCLOUD;
        }

        // 默认按YonBIP高级版处理（向后兼容）
        if (lowerCode.contains("yonbip")) {
            return YONBIP_ADVANCED;
        }

        return UNKNOWN;
    }

    /**
     * 获取此版本支持的所有应用代码
     */
    public List<String> getSupportedAppCodes() {
        return List.copyOf(supportedAppCodes);
    }

    /**
     * 检查指定应用代码是否被此版本支持
     */
    public boolean supportsAppCode(String appCode) {
        if (appCode == null) return false;
        return supportedAppCodes.contains(appCode.toLowerCase()) ||
                fromAppCodeByPattern(appCode.toLowerCase()) == this;
    }
}