package win.ixuni.yonyoudatadict.adapter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import win.ixuni.yonyoudatadict.model.YonyouVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本适配器工厂
 * 根据用友版本选择合适的适配器
 */
@Component
public class VersionAdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(VersionAdapterFactory.class);

    private final Map<YonyouVersion, VersionAdapter> adapterMap = new HashMap<>();

    @Autowired
    public VersionAdapterFactory(List<VersionAdapter> adapters) {
        // 注册所有适配器
        for (VersionAdapter adapter : adapters) {
            adapterMap.put(adapter.getSupportedVersion(), adapter);
            logger.info("注册版本适配器: {} -> {}",
                    adapter.getSupportedVersion().getDisplayName(),
                    adapter.getClass().getSimpleName());
        }
    }

    /**
     * 根据应用代码检测版本信息
     */
    public YonyouVersion detectVersion(String appCode) {
        YonyouVersion version = YonyouVersion.fromAppCode(appCode);
        logger.info("检测到版本: {} (应用代码: {})", version.getDisplayName(), appCode);
        return version;
    }

    /**
     * 根据版本获取适配器
     */
    public VersionAdapter getAdapter(YonyouVersion version) {
        VersionAdapter adapter = adapterMap.get(version);

        if (adapter == null) {
            logger.warn("未找到版本 {} 的适配器，使用默认适配器", version.getDisplayName());
            // 返回YonBIP高级版适配器作为默认适配器
            adapter = adapterMap.get(YonyouVersion.YONBIP_ADVANCED);
        }

        if (adapter != null) {
            logger.debug("选择适配器: {} for version: {}",
                    adapter.getClass().getSimpleName(),
                    version.getDisplayName());
        }

        return adapter;
    }

    /**
     * 根据应用代码获取适配器
     */
    public VersionAdapter getAdapter(String appCode) {
        YonyouVersion version = YonyouVersion.fromAppCode(appCode);
        return getAdapter(version);
    }

    /**
     * 获取所有支持的版本
     */
    public Map<YonyouVersion, VersionAdapter> getAllAdapters() {
        return new HashMap<>(adapterMap);
    }

    /**
     * 检查是否支持指定应用代码
     */
    public boolean isAppCodeSupported(String appCode) {
        YonyouVersion version = YonyouVersion.fromAppCode(appCode);
        return isVersionSupported(version);
    }

    /**
     * 检查是否支持指定版本
     */
    public boolean isVersionSupported(YonyouVersion version) {
        return adapterMap.containsKey(version);
    }

}