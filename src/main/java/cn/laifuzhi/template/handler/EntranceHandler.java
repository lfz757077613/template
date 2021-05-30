package cn.laifuzhi.template.handler;

import cn.laifuzhi.template.model.tcp.BaseInfoReq;
import cn.laifuzhi.template.model.tcp.DataTypeEnum;
import com.google.common.collect.Maps;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Sharable
@Component
public class EntranceHandler extends SimpleChannelInboundHandler<BaseInfoReq> {

    @Resource
    private Map<String, SimpleChannelInboundHandler<? extends BaseInfoReq>> handlerMap;

    private ThreadPoolExecutor executor;
    private Map<DataTypeEnum, SimpleChannelInboundHandler<? extends BaseInfoReq>> req2HandlerMap;

    @PostConstruct
    private void init() {
        executor = new ThreadPoolExecutor(
                200,
                200,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(4096),
                new CustomizableThreadFactory("BrokerHandler"),
                new ThreadPoolExecutor.CallerRunsPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        log.info("BrokerHandler run directly");
                        super.rejectedExecution(r, executor);
                    }
                }
        );
        req2HandlerMap = Maps.newHashMap();
        req2HandlerMap.put(DataTypeEnum.PING, handlerMap.get(HandlerNames.PING_HANDLER));
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        executor.shutdown();
        while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.info("BrokerHandler await ...");
        }
        log.info("BrokerHandler shutdown");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseInfoReq req) throws Exception {
        executor.submit(() -> {
            try {
                SimpleChannelInboundHandler<? extends BaseInfoReq> handler = req2HandlerMap.get(req.dataType());
                if (handler == null) {
                    log.error("broker not support dataType:{}", req.dataType());
                    return;
                }
                handler.channelRead(ctx, req);
            } catch (Exception e) {
                // 每个单独的handler都应该自己捕获了异常，应该不会执行到这里。一旦执行到这里会造成连接关闭
                ctx.fireExceptionCaught(e);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught remoteAddress:{}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
