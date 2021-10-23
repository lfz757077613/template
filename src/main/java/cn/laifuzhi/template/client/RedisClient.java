package cn.laifuzhi.template.client;

import cn.laifuzhi.template.conf.StaticConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public final class RedisClient {
    private static final long LOCK_SECOND = 60;
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final String LOCK_SUCC = "OK";
    private static final Long UNLOCK_SUCC = 1L;
    @Resource
    private StaticConfig staticConfig;

    private JedisPool jedisPool;

    @PostConstruct
    private void init() {
        log.info("RedisClient init");
        // redisPool设置，一般不用redisCluster，因为一般redis集群前都有一个proxy，对使用方来说就是一个redis实例。各大redis云厂商都是这样
        // JedisPoolConfig继承commons-pool的默认配置，只设置了空闲逐出相关配置
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(10);
        jedisPoolConfig.setMaxTotal(30);
        // 最大建立连接等待时间，仅在BlockWhenExhausted为true时生效，默认-1无限阻塞
        jedisPoolConfig.setMaxWaitMillis(1500);
        // 连接耗尽时是否阻塞，false报异常，true阻塞直到超时，默认true
//        jedisPoolConfig.setBlockWhenExhausted(true);
        // 3000既是soTimeout也是connectionTimeout
        jedisPool = new JedisPool(jedisPoolConfig, "127.0.0.1", 6379, 3000);

//        //redisCluster设置
//        JedisPoolConfig jedisClusterPoolConfig = new JedisPoolConfig();
//        // 最大空闲数
//        jedisClusterPoolConfig.setMaxIdle(10);
//        // 连接池的最大数据库连接数
//        jedisClusterPoolConfig.setMaxTotal(30);
//        // 最大建立连接等待时间
//        jedisClusterPoolConfig.setMaxWaitMillis(1500);
//        // 逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
//        jedisClusterPoolConfig.setMinEvictableIdleTimeMillis(1800000);
//        // 每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
//        jedisClusterPoolConfig.setNumTestsPerEvictionRun(3);
//        // 逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
//        jedisClusterPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
//        // 连接空闲多久后释放，当空闲时间大于该值且空闲连接大于最大空闲连接数时释放
//        jedisClusterPoolConfig.setSoftMinEvictableIdleTimeMillis(10000);
//        // 是否在从池中取出连接前进行检验,如果检验失败,则从池中去除连接并尝试取出另一个
//        jedisClusterPoolConfig.setTestOnBorrow(true);
//        // 在空闲时检查有效性, 默认false
//        jedisClusterPoolConfig.setTestWhileIdle(true);
//        // 连接耗尽时是否阻塞，false报异常，true阻塞直到超时，默认true
//        jedisClusterPoolConfig.setBlockWhenExhausted(false);
//        Set<HostAndPort> nodeSet = Sets.newHashSet();
//        nodeSet.add(new HostAndPort("127.0.0.1", 6381));
//        nodeSet.add(new HostAndPort("127.0.0.1", 6382));
//        nodeSet.add(new HostAndPort("127.0.0.1", 6383));
//        nodeSet.add(new HostAndPort("127.0.0.1", 6384));
//        nodeSet.add(new HostAndPort("127.0.0.1", 6385));
//        nodeSet.add(new HostAndPort("127.0.0.1", 6386));
//        new JedisCluster(nodeSet, 2000, 100, jedisClusterPoolConfig);
//
        // redisson配置，基于netty，全链路异步可以选择使用这个
//        Config config = new Config();
//        config.useSingleServer()
//                .setAddress("redis://127.0.0.1:6379")
//                .setConnectionPoolSize(128)
//                .setConnectionMinimumIdleSize(32)
//                .setKeepAlive(true)
//                .setPingConnectionInterval(10000);
//        Redisson.create(config);
    }

    @PreDestroy
    private void destroy() {
        jedisPool.close();
    }

    public void set(String key, String value) {
        try (Jedis resource = jedisPool.getResource()) {
            resource.set(key, value);
        } catch (Exception e) {
            log.error("set error, key:{}, value:{}", key, value, e);
        }
    }

    // 使用redis实现的分布式锁，也可以直接用redisson实现的
    public Optional<String> tryLock(String key) {
        if (StringUtils.isBlank(key)) {
            return Optional.empty();
        }
        try (Jedis resource = jedisPool.getResource()) {
            String uuid = UUID.randomUUID().toString();
            String setRet = resource.set(key, uuid, SetParams.setParams().ex(LOCK_SECOND).nx());
            if (StringUtils.equals(setRet, LOCK_SUCC)) { // 设置成功
                return Optional.of(uuid);
            }
            // 设置失败
            return Optional.empty();
        } catch (Exception e) {
            log.error("lock error, key:{}", key, e);
            return Optional.empty();
        }
    }

    // redis分布式锁解锁
    public void unlock(String key, String uuid) {
        try (Jedis resource = jedisPool.getResource()) {
            Object result = resource.eval(UNLOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(uuid));
            log.info("unlock, key:{}, uuid:{} result:{}", key, uuid, result);
        } catch (Exception e) {
            log.error("unlock error, key:{}, uuid:{}", key, uuid, e);
        }
    }
}
