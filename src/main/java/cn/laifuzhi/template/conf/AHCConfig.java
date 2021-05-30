package cn.laifuzhi.template.conf;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AHCConfig {
    @Bean
    private static AsyncHttpClient asyncHttpClient() {
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setMaxRedirects(0)
                .setMaxRequestRetry(0)
                .setConnectTimeout(1000)
                .setReadTimeout(3000)
                .setRequestTimeout(3000)
                .setMaxConnections(-1)
                .setMaxConnectionsPerHost(-1)
                .setPooledConnectionIdleTimeout(60 * 1000)
                .setChunkedFileChunkSize(8192 * 2)
                .setHttpClientCodecMaxChunkSize(8192 * 2)
                .setHttpClientCodecMaxHeaderSize(8192 * 2)
                .setHttpClientCodecMaxInitialLineLength(4096 * 2)
                .setTcpNoDelay(true)
                .setSoReuseAddress(true)
                .setCompressionEnforced(false)
                .build();
        return new DefaultAsyncHttpClient(config);
    }
}
