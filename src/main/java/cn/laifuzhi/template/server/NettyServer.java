package cn.laifuzhi.template.server;

import cn.laifuzhi.template.handler.EntranceHandler;
import cn.laifuzhi.template.handler.ConnectHandler;
import cn.laifuzhi.template.handler.DataDecoder;
import cn.laifuzhi.template.handler.DataEncoder;
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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Slf4j
@Component
public class NettyServer {
    @Resource
    private EntranceHandler entranceHandler;

    private ServerBootstrap serverBootstrap;

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
                        p.addLast(new ConnectHandler(30, 0, 0));
                        p.addLast(new DataDecoder(2 * 1024 * 1024));
                        p.addLast(DataEncoder.INSTANCE);
                        p.addLast(entranceHandler);
                    }
                });
    }

    @PreDestroy
    private void destroy() {
        log.info("NettyServer shutdown ...");
        serverBootstrap.config().group().shutdownGracefully().awaitUninterruptibly();
        serverBootstrap.config().childGroup().shutdownGracefully().awaitUninterruptibly();
        log.info("NettyServer shutdown finished");
    }

    public void start() {
        serverBootstrap.bind().syncUninterruptibly();
    }
}
