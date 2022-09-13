package cn.laifuzhi.template.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class NettyServer {
    @Resource
    private ReqHandler reqHandler;
    @Resource
    private RespHandler respHandler;
    @Resource
    private PushHandler pushHandler;
    @Resource
    private ErrorHandler errorHandler;
    @Resource
    private ConnectManager connectManager;

    private AtomicBoolean stopped = new AtomicBoolean();
    private ServerBootstrap serverBootstrap;
    private ThreadPoolExecutor executorService;

    @PostConstruct
    private void init() {
        log.info("NettyServer init");
        // 只监听一个端口，bossGroup只设置一个线程就可以
        EventLoopGroup bossEventLoopGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerEventLoopGroup = new NioEventLoopGroup();
        Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
        // linux优先使用native能力，mac的kqueue是UnstableApi所以不使用
        if (Epoll.isAvailable()) {
            bossEventLoopGroup = new EpollEventLoopGroup(1);
            workerEventLoopGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        }
        executorService = new ThreadPoolExecutor(
                50,
                50,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new CustomizableThreadFactory(getClass().getSimpleName()));
        reqHandler.init(stopped, executorService);
        serverBootstrap = new ServerBootstrap().group(bossEventLoopGroup, workerEventLoopGroup)
                .channel(channelClass)
                .localAddress(8081)
                .option(ChannelOption.SO_BACKLOG, 256)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // 60s收不到client心跳认为连接假死(也就是收不到ping)，关闭channel
                        p.addLast(new ConnectHandler(30, 0, 0, connectManager));
                        p.addLast(new DataDecoder(2 * 1024 * 1024));
                        p.addLast(DataEncoder.INSTANCE);
                        p.addLast(reqHandler);
                        p.addLast(respHandler);
                        p.addLast(pushHandler);
                        p.addLast(errorHandler);
                    }
                });
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        if (!stopped.compareAndSet(false, true)) {
            log.info("nettyServer already closed or closing");
            return;
        }
        log.info("NettyServer shutdown boss group ...");
        // 拒绝新连接请求
        serverBootstrap.config().group().shutdownGracefully().awaitUninterruptibly(Long.MAX_VALUE);
        executorService.shutdown();
        while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("NettyServer executorService shutdown wait");
        }
        log.info("NettyServer waitAllChannelOutboundBufferFlush:{}", connectManager.waitAllChannelOutboundBufferFlush(5));
        // 断开当前所有链接
        log.info("NettyServer shutdown worker group ...");
        serverBootstrap.config().childGroup().shutdownGracefully().awaitUninterruptibly(Long.MAX_VALUE);
        log.info("NettyServer shutdown finished");
    }

    public void start() {
        serverBootstrap.bind().syncUninterruptibly();
    }
}
