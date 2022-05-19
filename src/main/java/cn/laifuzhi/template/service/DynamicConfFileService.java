package cn.laifuzhi.template.service;

import cn.laifuzhi.template.model.DynamicConfig;
import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 参考log4j的FileWatchdog工具类，jdk7后可以更优雅的通过WatchService实现
 * https://heapdump.cn/article/3778858 jdk bug造成感知不到文件更新，所以最好不要通过文件的更新时间判断，最好自己在文件内容中保存时间戳或版本号
 */
@Slf4j
@Component
public class DynamicConfFileService {
    private static final int REFRESH_PERIOD_SECOND = 5;

    private ScheduledExecutorService scheduledExecutor;
    private File file;
    private long lastModified;
    private volatile DynamicConfig dynamicConfig;

    @PostConstruct
    private void init() throws IOException {
        log.info("DynamicConfFileService init");
        file = Paths.get(new ApplicationHome(getClass()).getDir().getCanonicalPath(), "dynamicConf.json").toFile();
        refresh();
        if (dynamicConfig == null) {
            throw new RuntimeException("dynamicConfig null");
        }
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("DynamicConfFileService shutdown ...");
        scheduledExecutor.shutdown();
        while (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("DynamicConfFileService await ...");
        }
        log.info("DynamicConfFileService shutdown finished");
    }

    private synchronized void refresh() {
        try {
            long start = System.currentTimeMillis();
            if (!file.exists()) {
                log.error("DynamicConfFileService refresh not exist file:{}", file.getCanonicalPath());
                return;
            }
            long fileModified = file.lastModified();
            if (fileModified > lastModified) {
                dynamicConfig = JSON.parseObject(Files.asCharSource(file, StandardCharsets.UTF_8).read(), DynamicConfig.class);
                lastModified = fileModified;
                log.info("DynamicConfFileService refresh finish file:{} cost:{}", file.getCanonicalPath(), System.currentTimeMillis() - start);
            }
        } catch (Throwable t) {
            log.error("DynamicConfFileService refresh error", t);
        }
    }

    public void start() {
        scheduledExecutor.scheduleWithFixedDelay(this::refresh, REFRESH_PERIOD_SECOND, REFRESH_PERIOD_SECOND, TimeUnit.SECONDS);
    }

    public Optional<DynamicConfig> getDynamicConfig() {
        return Optional.ofNullable(dynamicConfig);
    }
}
