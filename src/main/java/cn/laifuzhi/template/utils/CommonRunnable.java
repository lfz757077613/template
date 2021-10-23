package cn.laifuzhi.template.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

@Slf4j
public class CommonRunnable implements Runnable {
    private final Map<String, String> mdcMap;
    private final CommonContext commonContext;
    private final Runnable runnable;

    public CommonRunnable(Runnable runnable) {
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
            if (mdcMap != null) {
                MDC.setContextMap(mdcMap);
            }
            runnable.run();
        } catch (Throwable t) {
            log.error("CommonRunnable error", t);
        } finally {
            MDC.clear();
            CommonContext.remove();
        }
    }
}
