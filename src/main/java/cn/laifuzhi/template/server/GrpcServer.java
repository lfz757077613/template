package cn.laifuzhi.template.server;

import cn.laifuzhi.template.conf.StaticConfig;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GrpcServer {
    @Resource
    private StaticConfig config;
    @Resource
    private GrpcMetadataHandler metadataHandler;

    private ThreadPoolExecutor executorService;
    private Server server;

    // executorService拒绝任务时会返回给客户端 http2.0 internal error
    @PostConstruct
    private void init() {
        log.info("GrpcServer init");
        executorService = new ThreadPoolExecutor(
                config.getGrpcThreadCount(),
                config.getGrpcThreadCount(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(config.getGrpcThreadQueue()),
                new CustomizableThreadFactory("GrpcServer"));
        server = NettyServerBuilder.forPort(config.getGrpcPort())
//                .addService(metadataHandler)
                .executor(executorService)
                .maxConnectionIdle(10, TimeUnit.MINUTES)
                .build();
        GlobalEventExecutor.INSTANCE.scheduleWithFixedDelay(() -> log.info("GrpcServer queue count:{}", executorService.getQueue().size()), 0, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("GrpcServer shutdown ...");
        // 会阻塞到所有任务执行完，除非和客户端的连接断了，断了的话不会执行线程池中未完成的任务
        server.shutdown().awaitTermination();
        executorService.shutdown();
        while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("GrpcServer shutdown wait queue:{}", executorService.getQueue().size());
        }
        log.info("GrpcServer shutdown finished");
    }

    public void start() throws IOException {
        server.start();
    }
}
