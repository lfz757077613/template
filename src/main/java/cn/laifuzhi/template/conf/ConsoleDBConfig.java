//package cn.laifuzhi.template.conf;
//
//import com.alibaba.messaging.ops2.model.OpsException;
//import com.google.common.collect.Maps;
//import com.taobao.common.keycenter.security.Cryptograph;
//import com.zaxxer.hikari.HikariDataSource;
//import lombok.AllArgsConstructor;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.mybatis.spring.mapper.MapperScannerConfigurer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import javax.sql.DataSource;
//import java.io.Closeable;
//import java.util.Map;
//
//@Configuration
//public class ConsoleDBConfig {
//    private static final ThreadLocal<String> REGION_KEY = new ThreadLocal<>();
//
//    @Bean
//    public ConsoleDataSource consoleDateSource(StaticConfig config, Cryptograph cryptograph) {
//        return new ConsoleDataSource(config, cryptograph);
//    }
//
//    @Bean
//    public DataSourceTransactionManager consoleTransactionManager(DataSource consoleDateSource) {
//        return new DataSourceTransactionManager(consoleDateSource);
//    }
//
//    @Bean
//    public SqlSessionFactory consoleSqlSessionFactory(StaticConfig config, DataSource consoleDateSource) throws Exception {
//        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
//        configuration.setDefaultStatementTimeout((int) config.getDefaultDBTimeout().getSeconds());
//        factoryBean.setConfiguration(configuration);
//        factoryBean.setDataSource(consoleDateSource);
//        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(config.getConsoleMapperLocation()));
//        return factoryBean.getObject();
//    }
//
//    @Bean
//    private static MapperScannerConfigurer consoleMapperScanner() {
//        MapperScannerConfigurer metadataMapperScanner = new MapperScannerConfigurer();
//        metadataMapperScanner.setSqlSessionFactoryBeanName("consoleSqlSessionFactory");
//        metadataMapperScanner.setBasePackage("com.alibaba.messaging.ops2.dao.console");
//        return metadataMapperScanner;
//    }
//
//    @Bean
//    public TransactionTemplate consoleTransactionTemplate(DataSourceTransactionManager consoleTransactionManager) {
//        return new TransactionTemplate(consoleTransactionManager);
//    }
//
//    public RegionCloseable putRegion(String region) {
//        if (REGION_KEY.get() != null) {
//            throw new OpsException("region already existed");
//        }
//        REGION_KEY.set(region);
//        return new RegionCloseable(region);
//    }
//
//    private static class ConsoleDataSource extends AbstractRoutingDataSource implements Closeable {
//        public ConsoleDataSource(StaticConfig config, Cryptograph cryptograph) {
//            Map<Object, Object> datasourceMap = Maps.newHashMap();
//            for (Map.Entry<String, StaticConfig.RegionConfig> entry : config.getRegionConfigMap().entrySet()) {
//                String region = entry.getKey();
//                StaticConfig.RegionConfig regionConfig = entry.getValue();
//                HikariDataSource consoleDataSource = new HikariDataSource();
//                consoleDataSource.setJdbcUrl(regionConfig.getConsoleDBUrl());
//                consoleDataSource.setUsername(regionConfig.getConsoleDBUsername());
//                consoleDataSource.setPassword(cryptograph.decrypt(regionConfig.getConsoleDBPassword(), config.getKeyCenterKeyName()));
//                consoleDataSource.setValidationTimeout(config.getValidationTimeout().toMillis());
//                consoleDataSource.setConnectionTimeout(config.getConnectionTimeout().toMillis());
//                consoleDataSource.setIdleTimeout(config.getIdleTimeout().toMillis());
//                consoleDataSource.setMaxLifetime(config.getMaxLifeTime().toMillis());
//                consoleDataSource.setMinimumIdle(0);
//                consoleDataSource.setMaximumPoolSize(config.getMaximumPoolSize());
//                datasourceMap.put(region, consoleDataSource);
//            }
//            setTargetDataSources(datasourceMap);
//        }
//
//        @Override
//        protected Object determineCurrentLookupKey() {
//            return REGION_KEY.get();
//        }
//
//        @Override
//        public void close() {
//            for (DataSource dataSource : getResolvedDataSources().values()) {
//                ((HikariDataSource) dataSource).close();
//            }
//        }
//    }
//
//    @AllArgsConstructor
//    public static class RegionCloseable implements Closeable {
//        private String region;
//        @Override
//        public void close() {
//            if (!StringUtils.equals(region, REGION_KEY.get())) {
//                throw new OpsException("region not match");
//            }
//            REGION_KEY.remove();
//        }
//    }
//}
