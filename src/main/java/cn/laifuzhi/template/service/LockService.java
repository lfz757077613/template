package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.LockInfoDao;
import cn.laifuzhi.template.model.PO.LockInfoPO;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

import static cn.laifuzhi.template.utils.Const.DATETIME_FORMAT;

@Slf4j
@Service
public class LockService {
    @Resource
    private LockInfoDao lockInfoDao;

    public Date tryLock(String lockKey, int expireSeconds) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey));
        try {
            Date now = new Date();
            Date newExpireTime = DateUtils.addSeconds(now, expireSeconds);
            LockInfoPO dbLockInfo = lockInfoDao.select(lockKey);
            if (dbLockInfo == null) {
                dbLockInfo = new LockInfoPO();
                dbLockInfo.setLockKey(lockKey);
                dbLockInfo.setExpireTime(newExpireTime);
                lockInfoDao.insert(dbLockInfo);
                return newExpireTime;
            }
            // 锁还未到过期时间，或者更新过期时间失败
            Date oldExpireTime = dbLockInfo.getExpireTime();
            if (now.before(oldExpireTime)
                    || lockInfoDao.updateExpireTime(lockKey, oldExpireTime, newExpireTime) <= 0) {
                return null;
            }
            log.info("tryLock key:{} expire:{}", lockKey, DateFormatUtils.format(newExpireTime, DATETIME_FORMAT));
            return newExpireTime;
        } catch (DuplicateKeyException e) {
            log.debug("tryLock duplicate key:{}", lockKey);
            return null;
        } catch (Exception e) {
            log.error("tryLock error key:{}", lockKey, e);
            return null;
        }
    }

    public void release(String lockKey, Date expectExpireTime) {
        Preconditions.checkArgument(StringUtils.isNotBlank(lockKey));
        try {
            boolean result = lockInfoDao.delete(lockKey, expectExpireTime) > 0;
            log.info("release key:{} expect:{} result:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT), result);
        } catch (Exception e) {
            log.error("release key error key:{} expect:{}", lockKey, DateFormatUtils.format(expectExpireTime, DATETIME_FORMAT), e);
        }
    }
}
