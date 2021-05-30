package cn.laifuzhi.template.handler;

import cn.laifuzhi.template.model.tcp.BaseDTO;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class DataEncoder extends MessageToByteEncoder<BaseDTO> {
    public static final DataEncoder INSTANCE = new DataEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, BaseDTO msg, ByteBuf out) throws Exception {
        Channel channel = ctx.channel();
        try {
            out.writeInt(DataDecoder.MAGIC_NUMBER);
            int lengthWriterIndex = out.writerIndex();
            out.writerIndex(lengthWriterIndex + Integer.BYTES);
            out = msg.encode(out);
            out.setInt(lengthWriterIndex, out.writerIndex() - DataDecoder.LENGTH_FIELD_OFFSET - DataDecoder.LENGTH_FIELD_LENGTH);
        } catch (Exception e) {
            log.error("encode error remoteAddress:{}", channel.remoteAddress(), e);
        }
    }
}
