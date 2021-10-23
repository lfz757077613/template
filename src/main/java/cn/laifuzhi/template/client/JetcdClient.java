package cn.laifuzhi.template.client;

import cn.laifuzhi.template.conf.StaticConfig;
import com.alibaba.fastjson.JSON;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

/**
 * 使用自定义配置文件：
 * docker run -itd -p 12379:12379 --name etcd -e ALLOW_NONE_AUTHENTICATION=yes -v /Users/laifuzhi/etcd.conf.yml:/opt/bitnami/etcd/conf/etcd.conf.yml -e ETCD_CONFIG_FILE=/opt/bitnami/etcd/conf/etcd.conf.yml bitnami/etcd:3.5.0
 * etcd.conf.yml如下：
 * advertise-client-urls: http://127.0.0.1:12379
 * listen-client-urls: http://0.0.0.0:12379
 * data-dir: /bitnami/etcd/data
 */
@Slf4j
@Component
public final class JetcdClient {
    @Resource
    private StaticConfig staticConfig;

    private Client client;

    @PostConstruct
    private void init() {
        log.info("JetcdClient init");
        client = Client.builder().endpoints("http://localhost:12379").build();
    }

    @PreDestroy
    private void destroy() {
        client.close();
    }

    public boolean put(String key, String value) {
        try {
            client.getKVClient().put(ByteSequence.from(key.getBytes()), ByteSequence.from(value.getBytes())).get();
            return true;
        } catch (Exception e) {
            log.error("jetcd put error key:{} value:{}", key, value, e);
            return false;
        }
    }

    public void watch(String key) throws ExecutionException, InterruptedException {
        ByteSequence keyByteSequence = ByteSequence.from(key.getBytes());
        GetResponse getResponse = client.getKVClient().get(keyByteSequence).get();
        long revision = getResponse.getHeader().getRevision();
        client.getWatchClient().watch(keyByteSequence, WatchOption.newBuilder().withRevision(revision).build(), new Watch.Listener() {
            @Override
            public void onNext(WatchResponse watchResponse) {
                for (WatchEvent event : watchResponse.getEvents()) {
                    System.out.println(Thread.currentThread().getName());
                    log.info("onNext event:{} {} {}", JSON.toJSONString(event), event.getKeyValue().getKey(), event.getKeyValue().getValue());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("onError", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("onCompleted");
            }
        });
    }
}
