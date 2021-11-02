package cn.laifuzhi.template.conf;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.config.ConfigFilter;
import com.alibaba.druid.filter.config.ConfigTools;
import com.alibaba.druid.filter.logging.Slf4jLogFilter;
import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.redisson.misc.Hash;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多数据源手动配置
 * hikari拿到连接后，如果连接已经超过500ms没用了，就会执行valid验证，所以设置keepalive没啥意义
 * druid是只有配置了testOnBorrow获取连接后才会验证(抄commons-pool的思路，命名都一样)
 */
@Configuration
public class MysqlConfig {
    private StaticConfig config;
    // @Bean的实例名就是方法名，并且会自动执行close、shutdown的public无参方法
//    @Bean
    public HikariDataSource dataSource() {
//       https://github.com/brettwooldridge/HikariCP/tree/dev
        HikariDataSource metadataDataSource = new HikariDataSource();
        metadataDataSource.setJdbcUrl(config.getMetadataDBUrl());
        metadataDataSource.setUsername(config.getMetadataDBUsername());
        metadataDataSource.setPassword(config.getMetadataDBPassword());
        metadataDataSource.setValidationTimeout(config.getValidationTimeout().toMillis());
        metadataDataSource.setConnectionTimeout(config.getConnectionTimeout().toMillis());
        metadataDataSource.setMaxLifetime(config.getMaxLifeTime().toMillis());
        // MinimumIdle默认等于MaximumPoolSize
//        metadataDataSource.setMinimumIdle();
        metadataDataSource.setMaximumPoolSize(config.getMaximumPoolSize());
        // IdleTimeout只有在MinimumIdle不等于MaximumPoolSize时才起作用
//        metadataDataSource.setIdleTimeout(config.getIdleTimeout().toMillis());
//        默认1，初始化连接池时如果有问题连接池会报异常启动不了
//        metadataDataSource.setInitializationFailTimeout();
        return metadataDataSource;
    }

    // java -cp druid-1.2.6.jar com.alibaba.druid.filter.config.ConfigTools root 获得加密后的密码和公私钥
    @Bean
    public DruidDataSource dataSource1() {
        // druid连接池默认testOnBorrow false，testOnReturn false, testOnIdle true
        // mysql的valid是ping不是执行sql，并且默认valid timeout是1s
        DruidDataSource metadataDataSource = new DruidDataSource();
        metadataDataSource.setUrl(config.getMetadataDBUrl());
        metadataDataSource.setUsername(config.getMetadataDBUsername());
        metadataDataSource.setPassword("K9fvs9N09wxHEmR5N8x2ps+TsN3fq/vZpPfdKIHpGHBBUXdiYvKjcR+DArVvKDtNYME7rUj/Dy5OfLYTIWdaww==");
        metadataDataSource.setInitialSize(config.getMaximumPoolSize() / 2);
        metadataDataSource.setMaxActive(config.getMaximumPoolSize());
        metadataDataSource.setMaxWait(2000);
        metadataDataSource.setUseUnfairLock(true);
//        metadataDataSource.setValidationQueryTimeout(1);
//        metadataDataSource.setValidationQuery("select 1");
        // 默认不打印真正执行的sql日志
        // https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_LogFilter
        Slf4jLogFilter slf4jLogFilter = new Slf4jLogFilter();
        slf4jLogFilter.setStatementExecutableSqlLogEnable(true);
        // 默认3s是慢查询，不打印慢查询日志
        // https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_StatFilter
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(5000);
        statFilter.setLogSlowSql(true);
        // 默认命中时会抛出异常、不打错误日志
        // https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE-wallfilter
        WallFilter wallFilter = new WallFilter();
        wallFilter.setLogViolation(true); // 命中打error日志
        WallConfig wallConfig = new WallConfig();
        wallConfig.setDeleteWhereNoneCheck(true);
        wallFilter.setConfig(wallConfig);
        ConfigFilter configFilter = new ConfigFilter();
        metadataDataSource.addConnectionProperty(ConfigFilter.CONFIG_DECRYPT, String.valueOf(Boolean.TRUE));
        metadataDataSource.addConnectionProperty(ConfigFilter.CONFIG_FILE, "classpath:druid-password.properties");
        // stat在wall前，stat的统计时间会包含wall
        metadataDataSource.setProxyFilters(Lists.newArrayList(slf4jLogFilter, statFilter, wallFilter, configFilter));
        return metadataDataSource;
    }

    @Bean
    public DataSourceTransactionManager transactionManager1(DataSource dataSource1) {
        return new DataSourceTransactionManager(dataSource1);
    }

    // spring提供的工具类，比用@Transaction更加灵活一些，默认PROPAGATION_REQUIRED
    // 可以在业务逻辑里随时new TransactionTemplate，然后设置超时、传播级别。不一定要单例，该类不是重资源
    // https://zhuanlan.zhihu.com/p/256263914
    @Bean
    public TransactionTemplate transactionTemplate1(DataSourceTransactionManager transactionManager1) {
        return new TransactionTemplate(transactionManager1);
    }

    // PROPAGATION_REQUIRES_NEW
    @Bean
    public TransactionTemplate transactionTemplate4New1(DataSourceTransactionManager transactionManager1) {
        TransactionTemplate template = new TransactionTemplate(transactionManager1);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    // PROPAGATION_NOT_SUPPORTED
    @Bean
    public TransactionTemplate transactionTemplate4NotSupport1(DataSourceTransactionManager transactionManager1) {
        TransactionTemplate template = new TransactionTemplate(transactionManager1);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        return template;
    }

    // DataSourceInitializer实现了InitializingBean，spring实例化该对象后，afterPropertiesSet中会执行初始化sql
    @Bean
    public DataSourceInitializer dataSourceInitializer1(DataSource dataSource1) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(dataSource1);
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
        resourceDatabasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        resourceDatabasePopulator.addScript(new PathMatchingResourcePatternResolver().getResource(config.getMetadataDBInitLocation()));
        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
        return dataSourceInitializer;
    }

    // mybatis需要等数据库初始化后再初始化
    @Bean
    @DependsOn("dataSourceInitializer1")
    public SqlSessionFactory sqlSessionFactory1(DataSource dataSource1) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setDefaultStatementTimeout((int) config.getDefaultDBTimeout().getSeconds());
        factoryBean.setConfiguration(configuration);
        factoryBean.setDataSource(dataSource1);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(config.getMetadataMapperLocation()));
        return factoryBean.getObject();
    }

    @Bean
    public MapperScannerConfigurer mapperScanner1() {
        MapperScannerConfigurer metadataMapperScanner = new MapperScannerConfigurer();
        metadataMapperScanner.setSqlSessionFactoryBeanName("sqlSessionFactory1");
        metadataMapperScanner.setBasePackage("cn.laifuzhi.template.dao");
        return metadataMapperScanner;
    }
//
//    @Bean
//    public MultipleDataSource multipleDataSource(DataSource dataSource1) {
//        return new MultipleDataSource(dataSource1);
//    }
//
//    public static class MultipleDataSource extends AbstractRoutingDataSource {
//        public static final String DATA_SOURCE_A = "dataSourceTestA";  // A库
//        public static final String DATA_SOURCE_B = "dataSourceTestB";  // B库
//
//        private static final ThreadLocal<String> dataSourceKey = new ThreadLocal<String>();
//
//        MultipleDataSource(DataSource dataSource) {
//            Map<Object, Object> map = new HashMap<>();
//            map.put(DATA_SOURCE_A, dataSource);
//            setTargetDataSources(map);
//        }
//
//        public static void setDataSourceKey(String dataSource) {
//            dataSourceKey.set(dataSource);
//        }
//
//        @Override
//        protected Object determineCurrentLookupKey() {
//            return dataSourceKey.get();
//        }
//
//        public static void clear(){
//            dataSourceKey.remove();
//        }
//    }

}
