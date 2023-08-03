package cn.laifuzhi.template.matrix;

import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DirectMemReporter {
    // netty中内存池、优先堆外内存、堆外内存大小限制、堆外内存管理方式均可设置
    // https://www.jianshu.com/p/29a89bf124c5
    // https://stackoverflow.com/questions/67068818/oom-killed-jvm-with-320-x-16mb-netty-directbytebuffer-objects
    // https://github.com/netty/netty/commit/eb1d12c75736c06525d42dab9c7b2e7b7a33eb6d 这个提交之后才可以直接获取netty的堆外内存使用量，之前只能通过反射获取
    public void start() {
        GlobalEventExecutor.INSTANCE.scheduleWithFixedDelay(
                () -> log.info("server direct memory size:{}b, max:{}", PlatformDependent.usedDirectMemory(), PlatformDependent.maxDirectMemory()),
                5, 5, TimeUnit.SECONDS);
    }
}
