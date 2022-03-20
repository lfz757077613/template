package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class CommonConsumer<T> implements Consumer<T> {
    private final Thread curThread;
    private final Map<String, String> mdcMap;
    private final Consumer<T> consumer;

    public CommonConsumer(Consumer<T> consumer) {
        this.curThread = Thread.currentThread();
        this.mdcMap = MDC.getCopyOfContextMap();
        this.consumer = consumer;
    }

    @Override
    public void accept(T t) {
        try {
            if (curThread != Thread.currentThread() && mdcMap != null) {
                MDC.setContextMap(mdcMap);
            }
            consumer.accept(t);
        } finally {
            if (curThread != Thread.currentThread() && mdcMap != null) {
                MDC.clear();
            }
        }
    }
}
