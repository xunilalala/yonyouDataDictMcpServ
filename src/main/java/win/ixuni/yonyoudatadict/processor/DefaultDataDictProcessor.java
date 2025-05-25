package win.ixuni.yonyoudatadict.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.ixuni.yonyoudatadict.model.DataDictDetail;

/**
 * 默认数据字典处理器
 * 不做任何修改，仅记录日志
 */
public class DefaultDataDictProcessor implements DataDictProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataDictProcessor.class);
    
    @Override
    public DataDictDetail process(DataDictDetail detail) {
        logger.info("处理数据字典详情: {}", detail.getDisplayName());
        return detail;
    }
}
