package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Resp;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

@Sharable
@Component
public class RespHandler extends SimpleChannelInboundHandler<Resp> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Resp resp) throws Exception {
        try {

        } catch (Throwable throwable) {

        }
    }
}
