package cn.laifuzhi.template.utils;

import ch.qos.logback.core.hook.ShutdownHookBase;
import cn.laifuzhi.template.Application;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

public class LogbackShutdownHook extends ShutdownHookBase {

    @SneakyThrows
    @Override
    public void run() {
        while (Application.isSpringActive()) {
            TimeUnit.SECONDS.sleep(1);
        }
        super.stop();
    }
}
