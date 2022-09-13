package cn.laifuzhi.template.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ConnectManager {
    private static final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void addChannel(Channel channel) {
        activeChannels.add(channel);
    }

    public boolean waitAllChannelOutboundBufferFlush(int maxWaitSecond) throws InterruptedException {
        for (long start = System.currentTimeMillis(); System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(maxWaitSecond); ) {
            TimeUnit.SECONDS.sleep(1);
            if (activeChannels.stream().allMatch(this::channelOutboundBufferEmpty)) {
                return true;
            }
        }
        return false;
    }

    private boolean channelOutboundBufferEmpty(Channel channel) {
        ChannelOutboundBuffer outboundBuffer = channel.unsafe().outboundBuffer();
        return outboundBuffer == null || outboundBuffer.isEmpty();
    }
}
