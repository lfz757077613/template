package cn.laifuzhi.template.RocketHttp;

import cn.laifuzhi.template.RocketHttp.model.RocketChannelWrapper;
import cn.laifuzhi.template.RocketHttp.model.RocketConfig;
import cn.laifuzhi.template.RocketHttp.model.RocketRequest;
import cn.laifuzhi.template.RocketHttp.model.RocketResponse;
import cn.laifuzhi.template.RocketHttp.reactive.RocketChannelConsumer;
import cn.laifuzhi.template.RocketHttp.reactive.RocketConnectListener;
import cn.laifuzhi.template.RocketHttp.reactive.RocketDIYConsumer;
import cn.laifuzhi.template.RocketHttp.reactive.RocketDIYHandler;
import cn.laifuzhi.template.RocketHttp.reactive.RocketNettyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.laifuzhi.template.RocketHttp.Utils.getSocketName;
import static cn.laifuzhi.template.RocketHttp.Utils.joinHostPort;
import static cn.laifuzhi.template.RocketHttp.Utils.splitHostPort;

@Slf4j
public final class RocketClient implements Closeable {
    public static final AttributeKey<CompletableFuture<RocketResponse>> FUTURE = AttributeKey.valueOf("FUTURE");
    private RocketConfig config;
    private final Bootstrap bootstrap;
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final RocketNettyHandler rocketNettyHandler = new RocketNettyHandler();

    public RocketClient(RocketConfig config) {
        this.config = config;
        // ??????Native transports?????????????????????????????????????????????????????????????????????????????????????????????????????????debug????????????????????????????????????????????????Successfully loaded the library
        // ????????????????????????netty_transport_native_kqueue_x86_64 cannot be loaded
        EventLoopGroup eventLoopGroup = null;
        Class<? extends Channel> channelClass = null;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup();
            channelClass = EpollSocketChannel.class;
        }
        if (KQueue.isAvailable()) {
            eventLoopGroup = new KQueueEventLoopGroup();
            channelClass = KQueueSocketChannel.class;
        }
        bootstrap = new Bootstrap().group(eventLoopGroup != null ? eventLoopGroup : new NioEventLoopGroup())
                .channel(channelClass != null ? channelClass : NioSocketChannel.class)
//                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
//                .option(ChannelOption.AUTO_CLOSE, false)
//                .option(ChannelOption.SO_REUSEADDR, true)
//                .option(ChannelOption.SO_KEEPALIVE, false)
//                .option(ChannelOption.TCP_NODELAY, true)
//                ??????AUTO_READ??????????????????????????????
//                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
//                .option(ChannelOption.SO_RCVBUF, 32 * 1024)
//                .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(config.getHttpMaxContent()))
                                .addLast(new HttpContentDecompressor())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(rocketNettyHandler);
                    }
                });

        GenericKeyedObjectPoolConfig<RocketChannelWrapper> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotal(config.getMaxConnect());
        poolConfig.setMaxTotalPerKey(config.getMaxConnectPerHost());
        poolConfig.setMaxIdlePerKey(config.getMaxConnectPerHost());
        poolConfig.setBlockWhenExhausted(config.isBlockWhenExhausted());
        poolConfig.setMaxWaitMillis(config.getBlockTimeout());
        poolConfig.setTestOnBorrow(true);
        // ???EvictIdleConnectPeriod?????????????????????????????????IdleConnectKeepAliveTime?????????????????????destroyObject
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getEvictIdleConnectPeriod());
        poolConfig.setMinEvictableIdleTimeMillis(config.getIdleConnectKeepAliveTime());
        // -1????????????????????????????????????????????????MinEvictableIdleTimeMillis????????????evict
        poolConfig.setNumTestsPerEvictionRun(-1);
        channelPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<String, RocketChannelWrapper>() {
            @Override
            public RocketChannelWrapper create(String key) throws Exception {
                log.debug("create key:{}", key);
                String[] hostPort = splitHostPort(key);
                return new RocketChannelWrapper(true, bootstrap.connect(hostPort[0], Integer.parseInt(hostPort[1])));
            }

            @Override
            public PooledObject<RocketChannelWrapper> wrap(RocketChannelWrapper value) {
                return new DefaultPooledObject<>(value);
            }

            @Override
            public void destroyObject(String key, PooledObject<RocketChannelWrapper> p) throws Exception {
                log.debug("destroyObject key:{} channel:{}", key, getSocketName(p.getObject().getConnectFuture().channel()));
                // ???????????????????????????close??????????????????syncUninterruptibly()
                p.getObject().getConnectFuture().channel().close();
            }

            @Override
            public boolean validateObject(String key, PooledObject<RocketChannelWrapper> p) {
                // ????????????testOnBorrow???true???????????????makeObject?????????????????????validateObject?????????false??????????????????
                // ?????????isFirstUsed???(?????????????????????)???validateObject????????????true
                log.debug("validateObject key:{} firstUsed:{} channel:{}",
                        key, p.getObject().isFirstUsed(), getSocketName(p.getObject().getConnectFuture().channel()));
                return p.getObject().isFirstUsed() || p.getObject().getConnectFuture().channel().isActive();
            }
        }, poolConfig);
    }

    public CompletableFuture<RocketResponse> execute(RocketRequest request, RocketDIYHandler diyHandler) {
        CompletableFuture<RocketResponse> result = new CompletableFuture<>();
        try {
            if (diyHandler != null) {
                result.whenComplete(new RocketDIYConsumer(diyHandler));
            }
            if (stopped.get()) {
                result.completeExceptionally(new IOException("RocketClient closed"));
                return result;
            }
            DefaultFullHttpRequest nettyHttpRequest = RocketRequest2NettyHttpRequest(request);
            String channelKey = joinHostPort(request.getHost(), request.getPort());
            RocketChannelWrapper channelWrapper = channelPool.borrowObject(channelKey);
            result.whenComplete(new RocketChannelConsumer(channelPool, channelWrapper, channelKey));
            if (channelWrapper.isFirstUsed()) {
                channelWrapper.setFirstUsed(false);
            }
            channelWrapper.getConnectFuture().channel().attr(FUTURE).set(result);
            channelWrapper.getConnectFuture().addListener(new RocketConnectListener(nettyHttpRequest));
            // ?????????????????????????????????(????????????????????????????????????????????????????????????)???????????????????????????????????????????????????
            int requestTimeout = request.getTimeout() > 0 ? request.getTimeout() : config.getRequestTimeout();
            bootstrap.config().group().schedule(() -> result.completeExceptionally(new TimeoutException("total timeout")), requestTimeout, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public CompletableFuture<RocketResponse> execute(RocketRequest request) {
        return execute(request, null);
    }

    private DefaultFullHttpRequest RocketRequest2NettyHttpRequest(RocketRequest rocketRequest) {
        ByteBuf body = Unpooled.EMPTY_BUFFER;
        if (rocketRequest.getBody() != null) {
            body = Unpooled.wrappedBuffer(rocketRequest.getBody());
        }
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(rocketRequest.getVersion(), rocketRequest.getMethod(), rocketRequest.getUri(), body);
        HttpHeaders headers = httpRequest.headers();
        if (rocketRequest.getHeaders() != null) {
            headers.set(rocketRequest.getHeaders());
        }
        if (!headers.contains(HttpHeaderNames.HOST)) {
            headers.set(HttpHeaderNames.HOST, rocketRequest.getHost());
        }
        if (!headers.contains(HttpHeaderNames.USER_AGENT)) {
            headers.set(HttpHeaderNames.USER_AGENT, "RocketClient");
        }
        if (!headers.contains(HttpHeaderNames.ACCEPT)) {
            headers.set(HttpHeaderNames.ACCEPT, "*/*");
        }
        return httpRequest;
    }

    @Override
    public void close() {
        if (stopped.compareAndSet(false, true)) {
            log.info("RocketClient closing...");
            bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
            channelPool.close();
        }
    }
}
