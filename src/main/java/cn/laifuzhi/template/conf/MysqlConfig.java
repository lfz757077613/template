package cn.laifuzhi.template.conf;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 多数据源手动配置
 */
//@Configuration
public class MysqlConfig {
    // @Bean的实例名就是方法名，并且会自动执行close、shutdown的public无参方法
    @Bean
    private static HikariDataSource metadataDataSource(StaticConfig config) {
        HikariDataSource metadataDataSource = new HikariDataSource();
        metadataDataSource.setJdbcUrl(config.getMetadataDBUrl());
        metadataDataSource.setUsername(config.getMetadataDBUsername());
        metadataDataSource.setPassword(config.getMetadataDBPassword());
        metadataDataSource.setValidationTimeout(config.getValidationTimeout());
        metadataDataSource.setConnectionTimeout(config.getConnectionTimeout());
        metadataDataSource.setMaxLifetime(config.getMaxLifeTime());
        metadataDataSource.setMaximumPoolSize(config.getMaximumPoolSize());
        metadataDataSource.setKeepaliveTime(config.getKeepaliveTimeout());
        return metadataDataSource;
    }

    @Bean
    private static DataSourceInitializer metadataDataSourceInitializer(DataSource metadataDataSource, StaticConfig config) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(metadataDataSource);
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
        resourceDatabasePopulator.addScript(config.getMetadataDBInit());
        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
        return dataSourceInitializer;
    }

    @Bean
    private static DataSourceTransactionManager metadataTransactionManager(DataSource metadataDataSource) {
        return new DataSourceTransactionManager(metadataDataSource);
    }

    // mybatis需要等数据库初始化后再初始化
    @Bean
    @DependsOn("metadataDataSourceInitializer")
    private static SqlSessionFactory metadataSqlSessionFactory(DataSource metadataDataSource, StaticConfig config) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setDefaultStatementTimeout(config.getDefaultDBTimeout());
        factoryBean.setConfiguration(configuration);
        factoryBean.setDataSource(metadataDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(config.getMetadataMapperLocation()));
        return factoryBean.getObject();
    }

    @Bean
    private static SqlSessionTemplate metadataSqlSessionTemplate(SqlSessionFactory metadataSqlSessionFactory) {
        return new SqlSessionTemplate(metadataSqlSessionFactory);
    }

    @Bean
    private static MapperScannerConfigurer metadataMapperScanner() {
        MapperScannerConfigurer metadataMapperScanner = new MapperScannerConfigurer();
        metadataMapperScanner.setSqlSessionFactoryBeanName("metadataSqlSessionFactory");
        metadataMapperScanner.setBasePackage("xxx");
        return metadataMapperScanner;
    }
}