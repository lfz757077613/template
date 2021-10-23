package cn.laifuzhi.template.netty;

import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyUtils {

    public static void writeChannel(Channel channel, Message data) {
        writeChannel(channel, data, false);
    }

    public static void writeChannel(Channel channel, Message data, boolean force) {
        try {
            if (!channel.isActive() || (!force && !channel.isWritable())) {
                return;
            }
            channel.writeAndFlush(data).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.debug("writeChannel error", future.cause());
                    future.channel().close();
                }
            });
        } catch (Exception e) {
            log.debug("writeChannel error", e);
        }
    }
}
