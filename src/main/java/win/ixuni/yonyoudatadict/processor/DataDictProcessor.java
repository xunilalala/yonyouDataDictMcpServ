package win.ixuni.yonyoudatadict.processor;

import win.ixuni.yonyoudatadict.model.DataDictDetail;

/**
 * 数据字典处理器接口
 * 用于链式处理数据字典详情
 */
public interface DataDictProcessor {
    
    /**
     * 处理数据字典详情
     * 
     * @param detail 数据字典详情
     * @return 处理后的数据字典详情，如果返回null则中断链式处理
     */
    DataDictDetail process(DataDictDetail detail);
}
