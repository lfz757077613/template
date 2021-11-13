package cn.laifuzhi.template.cron;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolTaskScheduler关闭时会最多等待1秒等待任务都完成
 * 因为只使用ThreadPoolTaskScheduler触发cron任务，实际执行线程在别的线程池，所以ThreadPoolTaskScheduler基本不会堆积
 * spring关闭时会先关闭ThreadPoolTaskScheduler再关闭依赖schedule的组件
 * tomcat的ThreadPoolExecutor和TaskQueue可以先创建线程，再添加队列。通过重写TaskQueue的offer方法和ThreadPoolExecutor的execute方法
 * 不过感觉没有必要，只要用原生ThreadPoolExecutor，设置核心线程数和max一样，设置允许回收核心线程也能达到这个效果
 */
@Slf4j
@Component
public class MyScheduler {
    private ThreadPoolExecutor executorService;

    @PostConstruct
    private void init() {
        log.info("MyScheduler init");
        executorService = new ThreadPoolExecutor(
                10,
                10,
                10, TimeUnit.MINUTES,
                new SynchronousQueue<>(),
                new CustomizableThreadFactory(getClass().getSimpleName()));
        executorService.allowCoreThreadTimeOut(true);
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("MyScheduler shutdown ...");
        executorService.shutdown();
        while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("MyScheduler shutdown wait queue:{}", executorService.getQueue().size());
        }
        log.info("MyScheduler shutdown finished");
    }

    @Scheduled(cron = "0/10 * * * * ?")
    private void scheduler() {
        try {
            executorService.execute(() -> {
                try {
                    log.info("scheduler");
                    TimeUnit.SECONDS.sleep(5);
                } catch (Throwable e) {
                    log.error("scheduler error", e);
                }
            });
        } catch (Throwable e) {
            log.error("scheduler reject", e);
        }
    }
}
