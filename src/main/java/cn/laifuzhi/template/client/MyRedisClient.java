package cn.laifuzhi.template.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public final class MyRedisClient {
    private static final long LOCK_SECOND = 60;
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final String LOCK_SUCC = "OK";
    private static final Long UNLOCK_SUCC = 1L;

    @Resource
    private JedisPool jedisPool;

    public void set(String key, String value) {
        try (Jedis resource = jedisPool.getResource()){
            resource.set(key, value);
        } catch (Exception e) {
            log.error("set error, key:{}, value:{}", key, value, e);
        }
    }

    // redis cluster没有keys方法
    public Set<byte[]> keys(byte[] keyPatternByte) {
        throw new RuntimeException("redis cluster no keys method");
    }

    // 使用redis实现的分布式锁，也可以直接用redisson实现的
    public String tryLock(String key) {
        if (StringUtils.isBlank(key)) {
            return StringUtils.EMPTY;
        }
        try (Jedis resource = jedisPool.getResource()){
            String uuid = UUID.randomUUID().toString();
            String setRet = resource.set(key, uuid, SetParams.setParams().ex(LOCK_SECOND).nx());
            if (StringUtils.equals(setRet, LOCK_SUCC)) { // 设置成功
                return uuid;
            }
            // 设置失败
            return StringUtils.EMPTY;
        } catch (Exception e) {
            log.error("lock error, key:{}", key, e);
            return StringUtils.EMPTY;
        }
    }

    // redis分布式锁解锁
    public boolean unlock(String key, String uuid) {
        try (Jedis resource = jedisPool.getResource()) {
            Object result = resource.eval(UNLOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(uuid));
            return Objects.equals(UNLOCK_SUCC, result);
        } catch (Exception e) {
            log.error("unlock error, key:{}, uuid:{}", key, uuid, e);
            return false;
        }
    }
}
