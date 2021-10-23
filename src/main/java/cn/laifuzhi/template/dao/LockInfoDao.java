package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.LockInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.Optional;

public interface LockInfoDao {
    int insert(LockInfoPO lockInfo);

    int delete(@Param("lockKey") String lockKey, @Param("expireTime") Date expireTime);

    int updateExpireTime(@Param("lockKey") String lockKey,
                         @Param("newExpectTime") long newExpectTime,
                         @Param("oldExpectTime") long oldExpectTime);

    Optional<LockInfoPO> select(@Param("lockKey") String lockKey);
}
