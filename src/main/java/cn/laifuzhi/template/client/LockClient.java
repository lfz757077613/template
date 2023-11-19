package cn.laifuzhi.template.client;

import cn.laifuzhi.template.dao.LockInfoDao;
import cn.laifuzhi.template.model.PO.LockInfoPO;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static cn.laifuzhi.template.utils.Const.DATETIME_FORMAT;

@Slf4j
@Component
public class LockClient {
    @Resource
    private LockInfoDao lockInfoDao;
    @Resource(name = "transactionTemplate4NotSupport1")
    private TransactionTemplate transactionTemplate;

    public Optional<Long> tryLock(String lockKey, long expireSeconds) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey) && expireSeconds > 0);
        try {
            return transactionTemplate.execute(status -> {
                long now = System.currentTimeMillis();
                long newExpireTime = now + TimeUnit.SECONDS.toMillis(expireSeconds);
                Optional<LockInfoPO> optional = lockInfoDao.select(lockKey);
                if (!optional.isPresent()) {
                    LockInfoPO newLockInfo = new LockInfoPO();
                    newLockInfo.setLockKey(lockKey);
                    newLockInfo.setExpireTime(newExpireTime);
                    lockInfoDao.insert(newLockInfo);
                    log.debug("tryLock key:{} expire:{}", lockKey, DateFormatUtils.format(newExpireTime, DATETIME_FORMAT));
                    return Optional.of(newExpireTime);
                }
                LockInfoPO dbLockInfo = optional.get();
                long oldExpireTime = dbLockInfo.getExpireTime();
                if (now < oldExpireTime
                        || lockInfoDao.updateExpireTime(lockKey, newExpireTime, oldExpireTime) <= 0) {
                    // 锁还未到过期时间，或者更新过期时间失败
                    return Optional.empty();
                }
                // 锁已过期
                log.info("tryLock expire key:{} oldExpire:{} newExpire:{}",
                        lockKey, DateFormatUtils.format(oldExpireTime, DATETIME_FORMAT), DateFormatUtils.format(newExpireTime, DATETIME_FORMAT));
                return Optional.of(newExpireTime);
            });
        } catch (DuplicateKeyException e) {
            log.debug("tryLock duplicate key:{}", lockKey);
            return Optional.empty();
        } catch (Throwable t) {
            log.error("tryLock error key:{}", lockKey, t);
            return Optional.empty();
        }
    }

    public void release(String lockKey, long expectExpireTime) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey));
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (lockInfoDao.delete(lockKey, expectExpireTime) <= 0) {
                    log.error("release fail key:{} expect:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT));
                }
            });
        } catch (Exception e) {
            log.error("release key error key:{} expect:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT), e);
        }
    }
}
