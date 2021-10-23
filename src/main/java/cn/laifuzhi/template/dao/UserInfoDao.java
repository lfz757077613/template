package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.UserInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface UserInfoDao {
    int insert(UserInfoPO userInfo);

    Optional<UserInfoPO> selectByUid(@Param("uid") long uid);

    Optional<UserInfoPO> selectByUsername(@Param("username") String username);

    Optional<UserInfoPO> selectForShareByUid(@Param("uid")long uid);
}
