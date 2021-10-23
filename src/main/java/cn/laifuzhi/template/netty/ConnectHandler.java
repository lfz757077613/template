package cn.laifuzhi.template.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ConnectHandler extends IdleStateHandler {
    private ConnectManager connectManager;
    public ConnectHandler(int readerIdleTime, int writerIdleTime, int allIdleTime, ConnectManager connectManager) {
        super(readerIdleTime, writerIdleTime, allIdleTime, TimeUnit.MILLISECONDS);
        this.connectManager = connectManager;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        Channel channel = ctx.channel();
        log.info("channelIdle remoteAddress:{}", channel.remoteAddress());
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("channelActive remoteAddress:{}", channel.remoteAddress());
        connectManager.addChannel(channel);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("channelInactive remoteAddress:{}", channel.remoteAddress());
        super.channelInactive(ctx);
    }

    // channel不可写时关闭自动读取
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (channel.config().isAutoRead() != channel.isWritable()) {
            log.info("channelWritabilityChanged remoteAddress:{} writable:{}", channel.remoteAddress(), channel.isWritable());
            channel.config().setAutoRead(channel.isWritable());
        }
        super.channelWritabilityChanged(ctx);
    }
}
