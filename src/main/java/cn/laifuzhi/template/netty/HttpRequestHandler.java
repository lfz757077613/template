package cn.laifuzhi.template.netty;

import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandler.Sharable;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.SimpleChannelInboundHandler;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpRequest;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
@Sharable
@Component
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private File healthCheckFile;

    @PostConstruct
    private void init() throws IOException {
        healthCheckFile = Paths.get(new ApplicationHome(getClass()).getDir().getCanonicalPath(), "healthCheck").toFile();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        log.info("http receive uri:{}", uri);
        if (!StringUtils.equals("/healthCheck", uri)) {
            ctx.close();
            return;
        }
        FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
        // 如果不加Content-Length请求头，并且不是writeAndFlush就直接close
        // 则http请求会卡住，因为HttpObjectAggregator无法判断是否是完整响应
        HttpUtil.setContentLength(response, 0);
        if (!healthCheckFile.exists()) {
            response.setStatus(HttpResponseStatus.NOT_FOUND);
        }
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught", cause);
        ctx.close();
    }
}
