package cn.laifuzhi.template.matrix;

import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class DirectMemReporter {
    private static final String FILED_NAME = "DIRECT_MEMORY_COUNTER";

    private AtomicLong directMem;

    public DirectMemReporter() throws IllegalAccessException {
        Field field = ReflectionUtils.findField(PlatformDependent.class, FILED_NAME);
        Objects.requireNonNull(field).setAccessible(true);
        directMem = (AtomicLong) field.get(PlatformDependent.class);
    }

    public void start() {
        GlobalEventExecutor.INSTANCE.scheduleWithFixedDelay(
                () -> log.info("server direct memory size:{}b, max:{}", directMem.get(), PlatformDependent.maxDirectMemory()),
                5, 5, TimeUnit.SECONDS);
    }
}
