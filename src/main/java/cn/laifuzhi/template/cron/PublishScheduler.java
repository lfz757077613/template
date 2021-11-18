//package cn.laifuzhi.template.cron;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.messaging.ops2.common.client.LockClient;
//import com.alibaba.messaging.ops2.server.service.PublishServiceFacade;
//import com.alibaba.messaging.ops2.server.utils.CommonRunnable;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.slf4j.MDC;
//import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.SynchronousQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//import static com.alibaba.messaging.ops2.common.utils.Const.LockKey.PUBLISH_SCHEDULE_PRE;
//import static com.alibaba.messaging.ops2.common.utils.Const.MDC_TRACE_ID;
//
//@Slf4j
//@Component
//public class PublishScheduler {
//    @Resource
//    private LockClient lockClient;
//    @Resource
//    private PublishServiceFacade publishService;
//    @Resource(name = "opsTransactionTemplate")
//    private TransactionTemplate transactionTemplate;
//
//    private ScheduledExecutorService scheduledExecutor;
//    private ThreadPoolExecutor executorService;
//
//    @PostConstruct
//    private void init() {
//        log.info("PublishScheduler init");
//        scheduledExecutor = new ScheduledThreadPoolExecutor(1, new CustomizableThreadFactory(getClass().getSimpleName()));
//        executorService = new ThreadPoolExecutor(
//                300,
//                300,
//                1, TimeUnit.MINUTES,
//                new SynchronousQueue<>(),
//                new CustomizableThreadFactory(getClass().getSimpleName()),
//                new ThreadPoolExecutor.CallerRunsPolicy());
//        executorService.allowCoreThreadTimeOut(true);
//        scheduledExecutor.scheduleWithFixedDelay(new CommonRunnable(this::publishLoop), 5, 5, TimeUnit.SECONDS);
//        log.info("PublishScheduler init success");
//    }
//
//    @PreDestroy
//    private void destroy() throws InterruptedException {
//        log.info("PublishScheduler shutdown ...");
//        scheduledExecutor.shutdownNow();
//        while (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
//            log.info("PublishScheduler scheduledExecutor shutdown wait");
//        }
//        executorService.shutdownNow();
//        while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
//            log.info("PublishScheduler executorService shutdown wait queue:{}", executorService.getQueue().size());
//        }
//        log.info("PublishScheduler shutdown finished");
//    }
//
//    private void publishLoop() {
//        try {
//            List<PublishTask> unfinishedTaskList = selectAllUnfinished();
//            if (CollectionUtils.isEmpty(unfinishedTasks)) {
//                return;
//            }
//            for (PublishTask unfinishedTask : unfinishedTaskList) {
//                // 一主备的最大锁超时时间60分钟
//                String lockKey = PUBLISH_SCHEDULE_PRE + unfinishedTask.getClusterName;
//                Optional<Long> lock = lockClient.tryLock(lockKey, TimeUnit.MINUTES.toSeconds(60));
//                if (!lock.isPresent()) {
//                    continue;
//                }
//                // 一次调度仅执行一个集群的一批broker
//                try (MDC.putCloseable(MDC_TRACE_ID, unfinishedTask.getClusterName())) {
//                    log.info("publish schedule task:{}", JSON.toJSONString(unfinishedTask));
//                    List<PublishBrokerTask> brokerTaskList = selectByClusterOrderByName(unfinishedTask.getBatchCount());
//                    if (CollectionUtils.isEmpty(brokerTaskList)) {
//                        log.info("publish finished task:{}", JSON.toJSONString(unfinishedTask));
//                        publishTaskFinish(unfinishedTask);
//                        lockClient.release(lockKey, lock.get());
//                        continue;
//                    }
//                    CompletableFuture<Void> allBrokerTaskFuture = CompletableFuture.allOf(brokerTaskList.stream()
//                            .filter(publishBrokerTask -> publishBrokerTask.getSchedule() < System.currentTimeMillis())
//                            // publishService.fire()不能抛出异常
//                            .map(brokerTask -> CompletableFuture.runAsync(new CommonRunnable(() -> publishService.fire(brokerTask)), executorService))
//                            .toArray(CompletableFuture[]::new));
//                    allBrokerTaskFuture.thenRun(new CommonRunnable(() -> lockClient.release(lockKey, lock.get())));
//                }
//                break;
//            }
//        } catch (Exception e) {
//            log.error("publishLoop error", e);
//        }
//    }
//
//    private void publishTaskFinish(PublishTask task) {
//        try {
//            updateStateSuccess();
////        TODO 邮件，钉钉通知
//        } catch (Exception e) {
//
//        }
//    }
//}
