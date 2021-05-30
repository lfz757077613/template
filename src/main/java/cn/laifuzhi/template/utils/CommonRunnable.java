package cn.laifuzhi.template.utils;

import cn.laifuzhi.template.model.http.req.CommonContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

public class CommonRunnable implements Runnable{
    private final Map<String, String> mdcMap;
    private final CommonContext commonContext;
    private final Runnable runnable;

    public CommonRunnable(Runnable runnable) {
        Optional<CommonContext> contextOptional = CommonThreadLocal.get();
        if (!contextOptional.isPresent()) {
            throw new RuntimeException("no CommonContext");
        }
        this.mdcMap = MDC.getCopyOfContextMap();
        this.commonContext = contextOptional.get();
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            CommonThreadLocal.put(commonContext);
            if (mdcMap != null) {
                MDC.setContextMap(mdcMap);
            }
            runnable.run();
        } finally {
            MDC.clear();
        }
    }
}
