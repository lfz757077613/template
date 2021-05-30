package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.DynamicConfigPO;
import org.apache.ibatis.annotations.Param;

public interface DynamicConfigDao {
    DynamicConfigPO select(@Param("configKey") String configKey, @Param("version") long version);
}
