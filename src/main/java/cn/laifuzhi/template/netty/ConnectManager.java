package cn.laifuzhi.template.netty;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConnectManager {
    private static final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public void addChannel(Channel channel) {
        activeChannels.add(channel);
    }
}
