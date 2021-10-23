package cn.laifuzhi.template.client;

import cn.laifuzhi.template.conf.StaticConfig;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public final class HttpClient {
    @Resource
    private StaticConfig staticConfig;
    private AsyncHttpClient ahc;

    /**
     * ahc的默认配置在ahc-default.properties
     * 默认不会对3xx转发，最大重试请求5次，无限连接
     * 请求超时会关闭连接，否则已经返回了超时响应后，再收到服务端响应会有时序问题
     */
    @PostConstruct
    private void init() {
        log.info("HttpClient init");
        // 默认配置在jar包的ahc-default.properties中
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setMaxConnections(200)
                .setAcquireFreeChannelTimeout(2000)
                .setMaxRequestRetry(0)
                .setConnectTimeout(1000)
                .setReadTimeout(3000)
                .setRequestTimeout(3000)
                .setUseNativeTransport(true)
                .build();
        ahc = new DefaultAsyncHttpClient(config);
    }

    @PreDestroy
    private void destroy() throws IOException {
        ahc.close();
    }

}
