package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.OrderInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface OrderInfoDao {
    int insert(OrderInfoPO orderInfo);

    Optional<OrderInfoPO> select(@Param("uid") long uid, @Param("orderId") long orderId);

    Optional<OrderInfoPO> selectForUpdate(@Param("uid") long uid, @Param("orderId") long orderId);

    int updateState(@Param("uid") long uid, @Param("orderId") long orderId, @Param("state")int state);
}
