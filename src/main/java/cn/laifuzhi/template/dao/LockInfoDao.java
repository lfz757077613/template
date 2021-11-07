package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.LockInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.Optional;

public interface LockInfoDao {
    int insert(LockInfoPO lockInfo);

    int delete(@Param("lockKey") String lockKey, @Param("expireTime") long expireTime);

    int updateExpireTime(@Param("lockKey") String lockKey,
                         @Param("newExpireTime") long newExpireTime,
                         @Param("oldExpireTime") long oldExpireTime);

    Optional<LockInfoPO> select(@Param("lockKey") String lockKey);
}
