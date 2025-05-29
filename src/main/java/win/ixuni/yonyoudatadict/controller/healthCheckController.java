package win.ixuni.yonyoudatadict.controller;


import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import win.ixuni.yonyoudatadict.adapter.VersionAdapter;
import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.DataDictItem;
import win.ixuni.yonyoudatadict.model.YonyouVersion;
import win.ixuni.yonyoudatadict.service.DataDictService;
import win.ixuni.yonyoudatadict.util.DataDictDownloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/check")
public class healthCheckController {

    private final DataDictService dataDictService;

    private final DataDictDownloader dataDictDownloader;

    public healthCheckController(DataDictService dataDictService, DataDictDownloader dataDictDownloader) {
        this.dataDictService = dataDictService;
        this.dataDictDownloader = dataDictDownloader;
    }

    @RequestMapping()
    public String healthCheck() {
        return "OK";
    }

    // 测试工具方法：获取所有数据字典条目
    @RequestMapping("/tool/items")
    public List<DataDictItem> testToolGetAllItems() {
        return dataDictService.getDataDictItemsForController();
    }

    // 测试工具方法：根据类ID获取数据字典详情
    @RequestMapping("/tool/detail/{classId}")
    public DataDictDetail testToolGetDetail(
            @PathVariable("classId") String classId) {
        return dataDictService.getDataDictDetailForController(classId);
    }

    // 测试工具方法：根据名称搜索数据字典条目
    @RequestMapping("/tool/search")
    public List<DataDictItem> testToolSearch(
            @RequestParam(value = "name", required = false, defaultValue = "") String nameQuery) {
        return dataDictService.searchDataDictItemsByNameForController(nameQuery);
    }

    // 新增：动态添加应用代码支持的接口
    @RequestMapping("/test/add-support")
    public Map<String, Object> addAppCodeSupport(
            @RequestParam("version") String versionCode,
            @RequestParam("appCode") String appCode) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 查找版本
            YonyouVersion version = null;
            for (YonyouVersion v : YonyouVersion.values()) {
                if (v.getCode().equals(versionCode)) {
                    version = v;
                    break;
                }
            }

            if (version == null) {
                result.put("status", "ERROR");
                result.put("message", "未找到版本: " + versionCode);
                return result;
            }

            // 检查是否已经支持
            boolean alreadySupported = version.supportsAppCode(appCode);

            if (alreadySupported) {
                result.put("status", "INFO");
                result.put("message", "应用代码已被支持: " + appCode);
            } else {
                // 动态添加支持
                YonyouVersion.addAppCodeSupport(version, appCode);
                result.put("status", "SUCCESS");
                result.put("message", "成功添加应用代码支持: " + appCode + " -> " + version.getDisplayName());
            }

            result.put("version", version.getDisplayName());
            result.put("appCode", appCode);
            result.put("currentSupportedCodes", version.getSupportedAppCodes());

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "添加支持失败: " + e.getMessage());
        }

        return result;
    }

    // 新增：版本检测工具
    @RequestMapping("/architecture/detect")
    public Map<String, Object> detectVersion(@RequestParam("appCode") String appCode) {
        YonyouVersion version = YonyouVersion.fromAppCode(appCode);
        Map<String, Object> result = new HashMap<>();
        result.put("inputAppCode", appCode);
        result.put("detectedVersion", Map.of(
                "code", version.getCode(),
                "displayName", version.getDisplayName(),
                "parseType", version.getParseType()
        ));

        // 检查是否支持
        Map<YonyouVersion, VersionAdapter> supportedVersions = dataDictDownloader.getSupportedVersions();
        boolean supported = supportedVersions.containsKey(version);
        result.put("supported", supported);

        if (supported) {
            VersionAdapter adapter = supportedVersions.get(version);
            result.put("adapterInfo", Map.of(
                    "class", adapter.getClass().getSimpleName(),
                    "contentType", adapter.getContentType(),
                    "needsSpecialHeaders", adapter.needsSpecialHeaders()
            ));
        }

        return result;
    }

    // 新增：查看当前架构状态
    @RequestMapping("/architecture/status")
    public Map<String, Object> getArchitectureStatus() {
        Map<String, Object> status = new HashMap<>();

        // 当前版本信息
        YonyouVersion currentVersion = dataDictDownloader.getCurrentVersion();
        status.put("currentVersion", Map.of(
                "code", currentVersion.getCode(),
                "displayName", currentVersion.getDisplayName(),
                "parseType", currentVersion.getParseType()
        ));

        // 支持的所有版本
        Map<YonyouVersion, VersionAdapter> supportedVersions = dataDictDownloader.getSupportedVersions();
        Map<String, Object> versions = new HashMap<>();
        for (Map.Entry<YonyouVersion, VersionAdapter> entry : supportedVersions.entrySet()) {
            YonyouVersion version = entry.getKey();
            VersionAdapter adapter = entry.getValue();
            versions.put(version.getCode(), Map.of(
                    "displayName", version.getDisplayName(),
                    "parseType", version.getParseType(),
                    "adapterClass", adapter.getClass().getSimpleName(),
                    "contentType", adapter.getContentType()
            ));
        }
        status.put("supportedVersions", versions);

        // 缓存状态
        status.put("cacheStatus", Map.of(
                "detailCacheSize", dataDictDownloader.getDetailCacheSize(),
                "processorCount", dataDictDownloader.getProcessors().size()
        ));

        return status;
    }

    private String getPatternDescription(YonyouVersion version) {
        switch (version) {
            case YONBIP_FLAGSHIP:
                return "模式: yonbip\\d+r\\d+bip\\d+.*";
            case NCCLOUD:
                return "模式: ncc?ddc\\d+.*";
            case YONBIP_ADVANCED:
                return "模式: yonbip.*ddc";
            case NC65_OYONYOU:
                return "模式: nc.*65";
            default:
                return "无模式匹配";
        }
    }

    // 新增：旗舰版专用测试接口
    @RequestMapping("/flagship/test")
    public Map<String, Object> testFlagshipVersion() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 测试版本检测
            String testAppCode = "yonbip3r6bip2";
            YonyouVersion detectedVersion = YonyouVersion.fromAppCode(testAppCode);
            result.put("versionDetection", Map.of(
                    "testAppCode", testAppCode,
                    "detectedVersion", detectedVersion.getDisplayName(),
                    "parseType", detectedVersion.getParseType()
            ));

            // 测试适配器选择
            Map<YonyouVersion, VersionAdapter> supportedVersions = dataDictDownloader.getSupportedVersions();
            VersionAdapter flagshipAdapter = supportedVersions.get(YonyouVersion.YONBIP_FLAGSHIP);

            if (flagshipAdapter != null) {
                result.put("adapterInfo", Map.of(
                        "adapterClass", flagshipAdapter.getClass().getSimpleName(),
                        "contentType", flagshipAdapter.getContentType(),
                        "listUrl", flagshipAdapter.buildDictListUrl("https://media.oyonyou.com:18000/oyonyou/dict", testAppCode),
                        "detailUrlExample", flagshipAdapter.buildDetailUrl("https://media.oyonyou.com:18000/oyonyou/dict", testAppCode, "380714452628013056")
                ));

                // 测试当前配置
                YonyouVersion currentVersion = dataDictDownloader.getCurrentVersion();
                result.put("currentConfig", Map.of(
                        "currentVersion", currentVersion.getDisplayName(),
                        "isFlagshipMode", currentVersion == YonyouVersion.YONBIP_FLAGSHIP
                ));

                result.put("status", "SUCCESS");
                result.put("message", "旗舰版适配器测试成功");
            } else {
                result.put("status", "ERROR");
                result.put("message", "未找到旗舰版适配器");
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "旗舰版测试失败: " + e.getMessage());
        }

        return result;
    }

    // 新增：多版本对比测试接口
    @RequestMapping("/test/multi-version")
    public Map<String, Object> testMultiVersionSupport() {
        Map<String, Object> result = new HashMap<>();

        // 测试各种应用代码的版本检测
        String[] testCodes = {
                "yonbip3ddc",           // YonBIP高级版
                "yonbip3r6bip2",        // YonBIP旗舰版
                "nc65",                 // NC65
                "nccddc1909",           // NCCloud
                "unknown_format"        // 未知格式
        };

        Map<String, Object> detectionResults = new HashMap<>();
        Map<YonyouVersion, VersionAdapter> supportedVersions = dataDictDownloader.getSupportedVersions();

        for (String code : testCodes) {
            YonyouVersion version = YonyouVersion.fromAppCode(code);
            VersionAdapter adapter = supportedVersions.get(version);

            detectionResults.put(code, Map.of(
                    "detectedVersion", version.getDisplayName(),
                    "parseType", version.getParseType(),
                    "hasAdapter", adapter != null,
                    "adapterClass", adapter != null ? adapter.getClass().getSimpleName() : "无"
            ));
        }

        result.put("versionDetectionTest", detectionResults);
        result.put("totalSupportedVersions", supportedVersions.size());
        result.put("supportedVersionsList", supportedVersions.keySet().stream()
                .map(YonyouVersion::getDisplayName).toList());

        return result;
    }

    // 新增：NCCloud专用测试接口
    @RequestMapping("/nccloud/test")
    public Map<String, Object> testNCCloudVersion() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 测试版本检测
            String testAppCode = "nccddc1909";
            YonyouVersion detectedVersion = YonyouVersion.fromAppCode(testAppCode);
            result.put("versionDetection", Map.of(
                    "testAppCode", testAppCode,
                    "detectedVersion", detectedVersion.getDisplayName(),
                    "parseType", detectedVersion.getParseType()
            ));

            // 测试适配器选择
            Map<YonyouVersion, VersionAdapter> supportedVersions = dataDictDownloader.getSupportedVersions();
            VersionAdapter nccloudAdapter = supportedVersions.get(YonyouVersion.NCCLOUD);

            if (nccloudAdapter != null) {
                result.put("adapterInfo", Map.of(
                        "adapterClass", nccloudAdapter.getClass().getSimpleName(),
                        "contentType", nccloudAdapter.getContentType(),
                        "needsSpecialHeaders", nccloudAdapter.needsSpecialHeaders(),
                        "listUrl", nccloudAdapter.buildDictListUrl("", testAppCode),
                        "detailUrlExample", nccloudAdapter.buildDetailUrl("", testAppCode, "5083")
                ));

                // 测试当前配置
                YonyouVersion currentVersion = dataDictDownloader.getCurrentVersion();
                result.put("currentConfig", Map.of(
                        "currentVersion", currentVersion.getDisplayName(),
                        "isNCCloudMode", currentVersion == YonyouVersion.NCCLOUD
                ));

                result.put("status", "SUCCESS");
                result.put("message", "NCCloud适配器测试成功");
            } else {
                result.put("status", "ERROR");
                result.put("message", "未找到NCCloud适配器");
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "NCCloud测试失败: " + e.getMessage());
        }

        return result;
    }

    // 新增：测试多对一映射关系的接口
    @RequestMapping("/test/version-mapping")
    public Map<String, Object> testVersionMapping() {
        Map<String, Object> result = new HashMap<>();

        // 测试各种应用代码的版本映射
        String[] testCodes = {
                // YonBIP旗舰版的多个小版本
                "yonbip3r5bip2",
                "yonbip3r6bip2",
                "yonbip3r7bip2",
                "yonbip3r8bip2",
                "yonbip3r9bip3",        // 未来可能的版本

                // NCCloud的多个版本
                "nccddc1909",
                "nccddc2005",
                "nccddc2105",
                "nccddc2205",           // 未来可能的版本

                // YonBIP高级版
                "yonbip3ddc",

                // NC65的多个别名
                "nc65",
                "nc0065",
                "nc65cloud",

                // 边界情况
                "unknown_format",
                "yonbip_new_format"
        };

        Map<String, Object> mappingResults = new HashMap<>();
        Map<String, Integer> versionCounts = new HashMap<>();

        for (String code : testCodes) {
            YonyouVersion version = YonyouVersion.fromAppCode(code);

            // 统计每个版本映射的代码数量
            versionCounts.merge(version.getDisplayName(), 1, Integer::sum);

            mappingResults.put(code, Map.of(
                    "detectedVersion", version.getDisplayName(),
                    "versionCode", version.getCode(),
                    "parseType", version.getParseType(),
                    "isExactMatch", version.getSupportedAppCodes().contains(code.toLowerCase()),
                    "isPatternMatch", !version.getSupportedAppCodes().contains(code.toLowerCase()) && version != YonyouVersion.UNKNOWN
            ));
        }

        result.put("mappingResults", mappingResults);
        result.put("versionCounts", versionCounts);

        // 展示每个版本支持的应用代码
        Map<String, Object> versionSupport = new HashMap<>();
        for (YonyouVersion version : YonyouVersion.values()) {
            if (version != YonyouVersion.UNKNOWN) {
                versionSupport.put(version.getDisplayName(), Map.of(
                        "explicitSupport", version.getSupportedAppCodes(),
                        "patternSupport", getPatternDescription(version)
                ));
            }
        }
        result.put("versionSupport", versionSupport);

        return result;
    }

    // 新增：版本切换测试接口
    @RequestMapping("/test/version-switch")
    public Map<String, Object> testVersionSwitch(@RequestParam("appCode") String appCode) {
        Map<String, Object> result = new HashMap<>();

        // 检测版本
        YonyouVersion version = YonyouVersion.fromAppCode(appCode);
        result.put("inputAppCode", appCode);
        result.put("detectedVersion", version.getDisplayName());

        // 获取适配器
        Map<YonyouVersion, VersionAdapter> adapters = dataDictDownloader.getSupportedVersions();
        VersionAdapter adapter = adapters.get(version);

        if (adapter != null) {
            result.put("adapterFound", true);
            result.put("adapterClass", adapter.getClass().getSimpleName());

            // 构建URL示例
            String baseUrl = "https://media.oyonyou.com:18000/oyonyou/dict";
            result.put("urls", Map.of(
                    "listUrl", adapter.buildDictListUrl(baseUrl, appCode),
                    "detailUrlExample", adapter.buildDetailUrl(baseUrl, appCode, "test-id")
            ));
        } else {
            result.put("adapterFound", false);
            result.put("message", "未找到适配的版本适配器");
        }

        return result;
    }
}
