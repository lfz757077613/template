package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class CommonSupplier<T> implements Supplier<T> {
    private final Thread curThread;
    private final Map<String, String> mdcMap;
    private final Supplier<T> supplier;

    public CommonSupplier(Supplier<T> supplier) {
        this.curThread = Thread.currentThread();
        this.mdcMap = MDC.getCopyOfContextMap();
        this.supplier = supplier;
    }

    @Override
    public T get() {
        try {
            if (curThread != Thread.currentThread() && mdcMap != null) {
                MDC.setContextMap(mdcMap);
            }
            return supplier.get();
        } finally {
            if (curThread != Thread.currentThread() && mdcMap != null) {
                MDC.clear();
            }
        }
    }
}
