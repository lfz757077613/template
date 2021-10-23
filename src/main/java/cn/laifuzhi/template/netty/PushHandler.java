package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Push;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

@Sharable
@Component
public class PushHandler extends SimpleChannelInboundHandler<Push> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Push push) throws Exception {
        try {

        } catch (Throwable throwable) {

        }
    }
}
