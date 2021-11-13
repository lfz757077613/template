package cn.laifuzhi.template.grpc;

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
                0,
                config.getGrpcThreadCount(),
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.getGrpcThreadQueue()),
                new CustomizableThreadFactory(getClass().getSimpleName()));
        executorService.allowCoreThreadTimeOut(true);

        server = NettyServerBuilder.forPort(config.getGrpcPort())
                .addService(metadataHandler)
                .executor(executorService)
                .maxConnectionIdle(10, TimeUnit.MINUTES)
                .build();
        GlobalEventExecutor.INSTANCE.scheduleWithFixedDelay(() -> log.info("GrpcServer queue count:{}", executorService.getQueue().size()), 0, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("GrpcServer shutdown ...");
        /*
          会阻塞到所有任务执行完，除非和所有客户端的连接都断了，都断了的话不会执行线程池中未完成的任务，所以需要等待线程池执行完毕
          1.关闭处理新建连接的boss线程池，给所有连接发送goaway再处理遗留请求再断开连接
          2.grpc client收到goaway会标记该链接不可用，再请求会新建连接
          3.所以在awaitTermination期间，client新建连接失败，但是可以收到遗留请求的响应，收到所有响应后，发送goaway给server(可能连接已经断了，debug日志会报错，不过无所谓)
            2021-09-23 17:13:41.843 DEBUG 29424 --- [-worker-ELG-3-2] i.g.n.s.i.n.h.c.h.Http2ConnectionHandler : [id: 0xe62f04d2, L:/127.0.0.1:52353 ! R:/127.0.0.1:10051] Sending GOAWAY failed: lastStreamId '0', errorCode '2', debugData 'Connection reset by peer'. Forcing shutdown of the connection.
            java.io.IOException: Broken pipe
                at sun.nio.ch.FileDispatcherImpl.writev0(Native Method)
                at sun.nio.ch.SocketDispatcher.writev(SocketDispatcher.java:51)
                at sun.nio.ch.IOUtil.write(IOUtil.java:148)
                at sun.nio.ch.SocketChannelImpl.write(SocketChannelImpl.java:504)
                at io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel.doWrite(NioSocketChannel.java:423)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannel$AbstractUnsafe.flush0(AbstractChannel.java:934)
                at io.grpc.netty.shaded.io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.flush0(AbstractNioChannel.java:354)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannel$AbstractUnsafe.flush(AbstractChannel.java:898)
                at io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$HeadContext.flush(DefaultChannelPipeline.java:1372)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeFlush0(AbstractChannelHandlerContext.java:750)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeFlush(AbstractChannelHandlerContext.java:742)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.flush(AbstractChannelHandlerContext.java:728)
                at io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2ConnectionHandler.onError(Http2ConnectionHandler.java:643)
                at io.grpc.netty.shaded.io.grpc.netty.AbstractNettyHandler.exceptionCaught(AbstractNettyHandler.java:94)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:302)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:281)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.fireExceptionCaught(AbstractChannelHandlerContext.java:273)
                at io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline$HeadContext.exceptionCaught(DefaultChannelPipeline.java:1377)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:302)
                at io.grpc.netty.shaded.io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:281)
                at io.grpc.netty.shaded.io.netty.channel.DefaultChannelPipeline.fireExceptionCaught(DefaultChannelPipeline.java:907)
                at io.grpc.netty.shaded.io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.handleReadException(AbstractNioByteChannel.java:125)
                at io.grpc.netty.shaded.io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:177)
                at io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:714)
                at io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:650)
                at io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:576)
                at io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:493)
                at io.grpc.netty.shaded.io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989)
                at io.grpc.netty.shaded.io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
                at io.grpc.netty.shaded.io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
                at java.lang.Thread.run(Thread.java:748)
         */
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
