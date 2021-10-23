package cn.laifuzhi.template.netty;

import io.grpc.netty.shaded.io.netty.bootstrap.ServerBootstrap;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.channel.ChannelInitializer;
import io.grpc.netty.shaded.io.netty.channel.ChannelPipeline;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.SocketChannel;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpContentCompressor;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpContentDecompressor;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObjectAggregator;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpServerCodec;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.grpc.netty.shaded.io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;


/**
 * http服务仅做健康检查，优雅上下线
 */
@Slf4j
@Component
// 先初始化grpcServer，后销毁grpcServer
@DependsOn("grpcServer")
public class HttpServer {
    @Resource
    private HttpRequestHandler httpRequestHandler;

    private ServerBootstrap bootstrap;

    @PostConstruct
    private void init() {
        bootstrap = new ServerBootstrap().group(new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new HttpServerCodec());
//                        自动处理keep-alive
                        pipeline.addLast(new HttpServerKeepAliveHandler());
//                        HttpContentCompressor仅压缩httpResponse,14年到现在还没有实现httpRequest的压缩
//                        https://github.com/netty/netty/issues/2132
                        pipeline.addLast(new HttpContentCompressor());
                        pipeline.addLast(new HttpContentDecompressor());
                        pipeline.addLast(new HttpObjectAggregator(512 * 1024));
//                        pipeline.addLast(new ChunkedWriteHandler());
//                        HttpContentCompressor和ChunkedWriteHandler同时使用会有问题，需要自定义一个byteBuf-httpContent类型转换handler
//                        https://stackoverflow.com/questions/20136334/netty-httpstaticfileserver-example-not-working-with-httpcontentcompressor
                        pipeline.addLast(httpRequestHandler);
                    }
                });
    }

    @PreDestroy
    private void destroy() {
        log.info("HttpServer shutdown ...");
        bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
    }

    public void bind() {
        ChannelFuture channelFuture = bootstrap.bind(new InetSocketAddress(9999)).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("HttpServer bind fail cause:", future.cause());
            }
        });
        channelFuture.syncUninterruptibly();
    }
}
