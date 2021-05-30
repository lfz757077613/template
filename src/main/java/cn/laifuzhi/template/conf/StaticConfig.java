package cn.laifuzhi.template.conf;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "template")
public final class StaticConfig {
    private Integer grpcPort;
    private Integer grpcThreadCount;
    private Integer grpcThreadQueue;
    private Integer validationTimeout;
    private Integer connectionTimeout;
    private Integer maxLifeTime;
    private Integer maximumPoolSize;
    private Integer keepaliveTimeout;

    private Integer defaultDBTimeout;
    private String metadataDBUrl;
    private String metadataDBUsername;
    private String metadataDBPassword;
    private String metadataMapperLocation;
    private transient Resource metadataDBInit; // Resource无法序列化
}
