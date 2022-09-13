package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Req;
import cn.laifuzhi.template.utils.CommonRunnable;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Sharable
@Component
public class ReqHandler extends SimpleChannelInboundHandler<Req> {
    private AtomicBoolean stopped;
    private ThreadPoolExecutor executor;

    public void init(AtomicBoolean stopped, ThreadPoolExecutor executor) {
        this.stopped = stopped;
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Req req) {
        if (stopped.get()) {
            // 可以把关闭连接改为返回一个已关闭响应
            context.close();
            return;
        }
        try {
            executor.execute(new CommonRunnable(() -> {
                if (executor.getQueue().size() > 10000) {
                    // 可以把关闭连接改为返回一个busy响应
                    context.close();
                    return;
                }
                // 逻辑处理
                if (!context.channel().isActive()) {
                    log.info("channel not active");
                    return;
                }
                NettyUtils.writeChannel(context.channel(), req);
            }));
        } catch (RejectedExecutionException e) {
            // 可以把关闭连接改为返回一个已关闭响应
            context.close();
        }
    }
}
