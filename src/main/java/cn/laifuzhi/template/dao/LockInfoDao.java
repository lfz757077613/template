package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.LockInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface LockInfoDao {
    int insert(LockInfoPO lockInfo);

    int delete(@Param("lockKey") String lockKey, @Param("expireTime") Date expireTime);

    int updateExpireTime(@Param("lockKey") String lockKey,
                         @Param("oldExpectTime") Date oldExpectTime,
                         @Param("newExpectTime") Date newExpectTime);

    LockInfoPO select(@Param("lockKey") String lockKey);
}
