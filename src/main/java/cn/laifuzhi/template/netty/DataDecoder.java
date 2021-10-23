package cn.laifuzhi.template.netty;

import cn.laifuzhi.template.netty.proto.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.zip.CRC32;

// 私有协议，前四字节是魔数，接着四个字节是数据长度，接着4个字节是数据包的crc32，接着是一个pb的完整数据包
// 亚马逊血泪教训，位反转造成系统崩溃，所以在应用协议层也做了数据校验
// http://noahdavids.org/self_published/CRC_and_checksum.html
@Slf4j
public class DataDecoder extends LengthFieldBasedFrameDecoder {
    public static final int MAGIC_NUMBER = 0xcafebeaf;
    public static final int LENGTH_FIELD_OFFSET = Integer.BYTES;
    public static final int LENGTH_FIELD_LENGTH = Integer.BYTES;
    public static final int CRC32_LENGTH = Integer.BYTES;

    public DataDecoder(int maxBytes) {
        super(maxBytes, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, CRC32_LENGTH, LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf byteBuf = null;
        Channel channel = ctx.channel();
        SocketAddress remoteAddress = channel.remoteAddress();
        try {
            //不是魔数开头直接拒绝访问
            if (in.readableBytes() >= Integer.BYTES && in.getInt(in.readerIndex()) != MAGIC_NUMBER) {
                throw new IOException("magic number error");
            }
            byteBuf = (ByteBuf) super.decode(ctx, in);
            if (byteBuf == null) {
                return null;
            }
            int actualCrc32 = byteBuf.readInt();
            byte[] data = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(data);
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            int expectCrc32 = (int) crc32.getValue();
            if (actualCrc32!=expectCrc32){
                throw new IOException("crc32 error");
            }
//            如果不需要数据校验，则无需再拷贝一遍byteBuf到byte[]
//            Payload payload = Payload.parseFrom(new ByteBufInputStream(byteBuf));
//            payload不会为null
            Payload payload = Payload.parseFrom(data);
            switch (payload.getDataCase()) {
                case REQ:
                    return payload.getReq();
                case RESP:
                    return payload.getResp();
                case PUSH:
                    return payload.getPush();
                case DATA_NOT_SET:
                default:
                    throw new IOException("decode error");
            }
        } catch (Exception e) {
            log.error("decode error remoteAddress:{}", remoteAddress, e);
            ctx.close();
            return null;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }
}
