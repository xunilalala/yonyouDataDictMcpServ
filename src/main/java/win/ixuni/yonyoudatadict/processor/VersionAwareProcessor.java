package win.ixuni.yonyoudatadict.processor;


import win.ixuni.yonyoudatadict.model.DataDictDetail;
import win.ixuni.yonyoudatadict.model.YonyouVersion;
import win.ixuni.yonyoudatadict.util.DataDictDownloader;

/**
 * 版本感知的数据字典处理器基类
 * 为不同版本提供统一的处理接口
 */
public abstract class VersionAwareProcessor implements DataDictProcessor {

    /**
     * 根据版本处理数据字典详情
     */
    @Override
    public final DataDictDetail process(DataDictDetail detail) {
        if (detail == null) {
            return null;
        }

        YonyouVersion version = getCurrentVersion();
        return processForVersion(detail, version);
    }

    /**
     * 获取当前版本
     */
    protected YonyouVersion getCurrentVersion() {
        try {
            DataDictDownloader downloader = DataDictDownloader.getInstance();
            if (downloader != null) {
                return downloader.getCurrentVersion();
            }
        } catch (Exception e) {
            // 忽略错误，使用默认版本
        }
        return YonyouVersion.YONBIP_ADVANCED; // 默认版本
    }

    /**
     * 检查是否为高级版
     */
    protected boolean isAdvancedVersion() {
        return getCurrentVersion() == YonyouVersion.YONBIP_ADVANCED;
    }

    /**
     * 检查是否为旗舰版
     */
    protected boolean isFlagshipVersion() {
        return getCurrentVersion() == YonyouVersion.YONBIP_FLAGSHIP;
    }

    /**
     * 子类需要实现的版本特定处理方法
     */
    protected abstract DataDictDetail processForVersion(DataDictDetail detail, YonyouVersion version);

}