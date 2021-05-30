package cn.laifuzhi.template.utils;

import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class DirectMemReporter {
    private ScheduledExecutorService scheduledExecutor;
    private static final String FILED_NAME = "DIRECT_MEMORY_COUNTER";
    private AtomicLong directMem;

    @PostConstruct
    private void init() throws IllegalAccessException {
        log.info("DirectMemReporter init");
        Field field = ReflectionUtils.findField(PlatformDependent.class, FILED_NAME);
        Objects.requireNonNull(field).setAccessible(true);
        directMem = (AtomicLong) field.get(PlatformDependent.class);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        report();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        scheduledExecutor.shutdown();
        while (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("DirectMemReporter await ...");
        }
        log.info("DirectMemReporter shutdown");
    }

    private void report() {
        log.info("server direct memory size:{}b, max:{}", directMem.get(), PlatformDependent.maxDirectMemory());
    }

    public void start() {
        scheduledExecutor.scheduleWithFixedDelay(
                () -> log.info("server direct memory size:{}b, max:{}", directMem.get(), PlatformDependent.maxDirectMemory()),
                5, 5, TimeUnit.SECONDS);

    }
}
