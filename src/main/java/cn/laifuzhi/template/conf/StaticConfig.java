package cn.laifuzhi.template.conf;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "template")
public final class StaticConfig {
    private Integer grpcPort;
    private Integer grpcThreadCount;
    private Integer grpcThreadQueue;
    private Duration validationTimeout;
    private Duration connectionTimeout;
    private Duration idleTimeout;
    private Duration maxLifeTime;
    private Integer maximumPoolSize;

    private Duration defaultDBTimeout;
    private String metadataDBUrl;
    private String metadataDBUsername;
    private String metadataDBPassword;
    private String metadataMapperLocation;
    private String metadataDBInitLocation;
}
