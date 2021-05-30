package cn.laifuzhi.template.conf;

import com.google.common.collect.Sets;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

//@Configuration
public class RedisConfig {
    // redisPool设置，一般不用redisCluster，因为一般redis集群前都有一个proxy，对使用方来说就是一个redis实例
    // 各大redis云厂商都是这样
    @Bean
    private static JedisPool jedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大空闲数
        jedisPoolConfig.setMaxIdle(10);
        // 连接池的最大数据库连接数
        jedisPoolConfig.setMaxTotal(30);
        // 最大建立连接等待时间
        jedisPoolConfig.setMaxWaitMillis(1500);
        // 逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
        jedisPoolConfig.setMinEvictableIdleTimeMillis(1800000);
        // 每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
        jedisPoolConfig.setNumTestsPerEvictionRun(3);
        // 逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
        // 连接空闲多久后释放，当空闲时间大于该值且空闲连接大于最大空闲连接数时释放
        jedisPoolConfig.setSoftMinEvictableIdleTimeMillis(10000);
        // 是否在从池中取出连接前进行检验,如果检验失败,则从池中去除连接并尝试取出另一个
        jedisPoolConfig.setTestOnBorrow(true);
        // 在空闲时检查有效性, 默认false
        jedisPoolConfig.setTestWhileIdle(true);
        // 连接耗尽时是否阻塞，false报异常，true阻塞直到超时，默认true
        jedisPoolConfig.setBlockWhenExhausted(false);
        return new JedisPool(jedisPoolConfig, "127.0.0.1", 6381, 3000, "password");
    }

    //redisCluster设置
    @Bean
    private static JedisCluster jedisCluster() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大空闲数
        jedisPoolConfig.setMaxIdle(10);
        // 连接池的最大数据库连接数
        jedisPoolConfig.setMaxTotal(30);
        // 最大建立连接等待时间
        jedisPoolConfig.setMaxWaitMillis(1500);
        // 逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
        jedisPoolConfig.setMinEvictableIdleTimeMillis(1800000);
        // 每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
        jedisPoolConfig.setNumTestsPerEvictionRun(3);
        // 逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
        // 连接空闲多久后释放，当空闲时间大于该值且空闲连接大于最大空闲连接数时释放
        jedisPoolConfig.setSoftMinEvictableIdleTimeMillis(10000);
        // 是否在从池中取出连接前进行检验,如果检验失败,则从池中去除连接并尝试取出另一个
        jedisPoolConfig.setTestOnBorrow(true);
        // 在空闲时检查有效性, 默认false
        jedisPoolConfig.setTestWhileIdle(true);
        // 连接耗尽时是否阻塞，false报异常，true阻塞直到超时，默认true
        jedisPoolConfig.setBlockWhenExhausted(false);
        Set<HostAndPort> nodeSet = Sets.newHashSet();
        nodeSet.add(new HostAndPort("127.0.0.1", 6381));
        nodeSet.add(new HostAndPort("127.0.0.1", 6382));
        nodeSet.add(new HostAndPort("127.0.0.1", 6383));
        nodeSet.add(new HostAndPort("127.0.0.1", 6384));
        nodeSet.add(new HostAndPort("127.0.0.1", 6385));
        nodeSet.add(new HostAndPort("127.0.0.1", 6386));
        return new JedisCluster(nodeSet, 2000, 100, jedisPoolConfig);
    }

    // redisson配置，基于netty，全链路异步可以选择使用这个
    @Bean
    private static RedissonClient redissonClient() {
        Config config = new Config();
        config.setThreads(0)
                .setNettyThreads(0)
                .useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setPassword("password")
                .setConnectionPoolSize(128)
                .setConnectionMinimumIdleSize(32)
                .setKeepAlive(true)
                .setPingConnectionInterval(10000);
        return Redisson.create(config);
    }

}
