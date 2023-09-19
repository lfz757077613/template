package cn.laifuzhi.template.dao;

import com.alibaba.messaging.ops2.model.PO.JsonTestPO;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface JsonTestDao {
    Optional<JsonTestPO> select(@Param("lockKey") String lockKey);
}
