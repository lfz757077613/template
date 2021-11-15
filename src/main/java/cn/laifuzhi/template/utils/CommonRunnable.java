package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class CommonRunnable implements Runnable {
    private final Thread curThread;
    private final Map<String, String> mdcMap;
    private final CommonContext commonContext;
    private final Runnable runnable;

    public CommonRunnable(Runnable runnable) {
        this.curThread = Thread.currentThread();
        this.mdcMap = MDC.getCopyOfContextMap();
        this.commonContext = CommonContext.get();
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            if (commonContext != null) {
                CommonContext.put(commonContext);
            }
            if (!Objects.equals(curThread, Thread.currentThread())) {
                if (commonContext != null) {
                    CommonContext.put(commonContext);
                }
                if (mdcMap != null) {
                    MDC.setContextMap(mdcMap);
                }
            }
            runnable.run();
        } catch (Throwable t) {
            log.error("CommonRunnable error", t);
            throw t;
        } finally {
            if (!Objects.equals(curThread, Thread.currentThread())) {
                if (commonContext != null) {
                    CommonContext.remove();
                }
                if (mdcMap != null) {
                    MDC.clear();
                }
            }
        }
    }
}
