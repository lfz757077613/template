package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Req;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

@Sharable
@Component
public class ReqHandler extends SimpleChannelInboundHandler<Req> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Req req) throws Exception {
        try {

        } catch (Throwable throwable) {

        }
    }
}
