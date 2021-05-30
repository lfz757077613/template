package cn.laifuzhi.template.handler;

import cn.laifuzhi.template.model.tcp.Ping;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Sharable
@Component(HandlerNames.PING_HANDLER)
public class PingHandler extends SimpleChannelInboundHandler<Ping> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Ping ping) throws Exception {
        Channel channel = ctx.channel();
    }
}
