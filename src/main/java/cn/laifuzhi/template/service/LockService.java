package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.LockInfoDao;
import cn.laifuzhi.template.model.PO.LockInfoPO;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static cn.laifuzhi.template.utils.Const.DATETIME_FORMAT;

@Slf4j
@Service
public class LockService {
    @Resource
    private LockInfoDao lockInfoDao;
    @Resource(name = "transactionTemplate4NotSupport1")
    private TransactionTemplate transactionTemplate4NotSupport;

    public Optional<Long> tryLock(String lockKey, int expireSeconds) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey) && expireSeconds > 0);
        try {
            return transactionTemplate4NotSupport.execute(status -> {
                long now = System.currentTimeMillis();
                long newExpireTime = now + TimeUnit.SECONDS.toMillis(expireSeconds);
                Optional<LockInfoPO> optional = lockInfoDao.select(lockKey);
                if (!optional.isPresent()) {
                    LockInfoPO newLockInfo = new LockInfoPO();
                    newLockInfo.setLockKey(lockKey);
                    newLockInfo.setExpireTime(newExpireTime);
                    lockInfoDao.insert(newLockInfo);
                    log.info("tryLock key:{} expire:{}", lockKey, DateFormatUtils.format(newExpireTime, DATETIME_FORMAT));
                    return Optional.of(newExpireTime);
                }
                LockInfoPO dbLockInfo = optional.get();
                // 锁还未到过期时间，或者更新过期时间失败
                long oldExpireTime = dbLockInfo.getExpireTime();
                if (now < oldExpireTime
                        || lockInfoDao.updateExpireTime(lockKey, newExpireTime, oldExpireTime) <= 0) {
                    return Optional.empty();
                }
                log.info("tryLock key:{} expire:{}", lockKey, DateFormatUtils.format(newExpireTime, DATETIME_FORMAT));
                return Optional.of(newExpireTime);
            });
        } catch (DuplicateKeyException e) {
            log.debug("tryLock duplicate key:{}", lockKey);
            return Optional.empty();
        } catch (Exception e) {
            log.error("tryLock error key:{}", lockKey, e);
            return Optional.empty();
        }
    }

    public void release(String lockKey, Date expectExpireTime) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey) && expectExpireTime != null);
        try {
            transactionTemplate4NotSupport.executeWithoutResult(status -> {
                boolean result = lockInfoDao.delete(lockKey, expectExpireTime) > 0;
                log.info("release key:{} expect:{} result:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT), result);
            });
       } catch (Exception e) {
            log.error("release key error key:{} expect:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT), e);
        }
    }
}
