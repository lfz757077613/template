package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.OrderInfoDao;
import cn.laifuzhi.template.dao.UserInfoDao;
import cn.laifuzhi.template.model.PO.OrderInfoPO;
import cn.laifuzhi.template.model.PO.UserInfoPO;
import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import cn.laifuzhi.template.model.enumeration.OrderStateEnum;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class OrderService {
    @Resource
    private OrderInfoDao orderInfoDao;
    @Resource
    private UserInfoDao userInfoDao;
    @Resource(name = "transactionTemplate1")
    private TransactionTemplate transactionTemplate;
    @Resource(name = "sqlSessionFactory1")
    private SqlSessionFactory sqlSessionFactory;

//    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, transactionManager = "transactionManager1")
    public BizCodeEnum saveOrder(OrderInfoPO orderInfo) {
        try {
            return transactionTemplate.execute(status -> {
                Optional<UserInfoPO> optional = userInfoDao.selectForShareByUid(orderInfo.getUid());
                if (!optional.isPresent()) {
                    status.setRollbackOnly();
                    return BizCodeEnum.USER_NOT_EXISTED;
                }
                orderInfoDao.insert(orderInfo);
                return BizCodeEnum.OK;
            });
        } catch (DuplicateKeyException e) {
            log.error("saveOrder duplicate, order:{}", JSON.toJSONString(orderInfo), e);
            return BizCodeEnum.ORDER_DUPLICATE;
        } catch (Exception e) {
            log.error("saveOrder error, order:{}", JSON.toJSONString(orderInfo), e);
            return BizCodeEnum.SYSTEM_ERROR;
        }
    }

    // 事务不受spring管理
    // https://github.com/mybatis/mybatis-3/wiki/FAQ#how-do-i-code-a-batch-insert
    public void batchInsert(List<OrderInfoPO> orderInfoPOList) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            OrderInfoDao orderInfoDao = sqlSession.getMapper(OrderInfoDao.class);
            for (OrderInfoPO orderInfoPO : orderInfoPOList) {
                orderInfoDao.insert(orderInfoPO);
            }
            sqlSession.commit();
        }
    }

    public BizCodeEnum payOrder(long uid, long orderId) {
        try {
            return transactionTemplate.execute(status -> {
                Optional<UserInfoPO> userInfoOptional = userInfoDao.selectForShareByUid(uid);
                if (!userInfoOptional.isPresent()) {
                    status.setRollbackOnly();
                    return BizCodeEnum.USER_NOT_EXISTED;
                }
                Optional<OrderInfoPO> orderInfoOptional = orderInfoDao.selectForUpdate(orderId, uid);
                if (!orderInfoOptional.isPresent()) {
                    status.setRollbackOnly();
                    return BizCodeEnum.ORDER_NOT_EXISTED;
                }
                OrderInfoPO orderInfo = orderInfoOptional.get();
                if (Objects.equals(OrderStateEnum.PAID.getState(), orderInfo.getState())) {
                    status.setRollbackOnly();
                    return BizCodeEnum.ORDER_STATE_WRONG;
                }
                orderInfoDao.updateState(uid, orderId, OrderStateEnum.PAID.getState());
                return BizCodeEnum.OK;
            });
        } catch (Exception e) {
            log.error("payOrder error, uid:{} orderId:{}", uid, orderId, e);
            return BizCodeEnum.SYSTEM_ERROR;
        }
    }
}
