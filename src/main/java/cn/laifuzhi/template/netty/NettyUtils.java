package cn.laifuzhi.template.netty;

import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyUtils {

    public static void writeChannel(Channel channel, Message data) {
        writeChannel(channel, data, true);
    }

    public static void writeChannel(Channel channel, Message data, boolean force) {
        try {
            if (!channel.isActive() || (!force && !channel.isWritable())) {
                log.info("channel not active");
                return;
            }
            if (!force && !channel.isWritable()) {
                log.info("channel not writable");
                return;
            }
            channel.writeAndFlush(data).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("writeChannel error", future.cause());
                    future.channel().close();
                }
            });
        } catch (Exception e) {
            // channel对应的eventloop线程关闭了的话，addListener会抛出RejectedExecutionException异常
            log.debug("writeChannel error", e);
        }
    }
}
