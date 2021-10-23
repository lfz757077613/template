package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.zip.CRC32;

@Slf4j
@Sharable
public class DataEncoder extends MessageToByteEncoder<Payload> {
    public static final DataEncoder INSTANCE = new DataEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, Payload payload, ByteBuf out) throws Exception {
        out.writeInt(DataDecoder.MAGIC_NUMBER);
//        如果不需要数据校验，则无需再拷贝一遍byte[] data = payload.toByteArray();
//        out.writeInt(payload.getSerializedSize());
//        payload.writeTo(new ByteBufOutputStream(out));
        byte[] data = payload.toByteArray();
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        out.writeInt(data.length);
        out.writeInt((int) crc32.getValue());
        out.writeBytes(data);
    }
}
