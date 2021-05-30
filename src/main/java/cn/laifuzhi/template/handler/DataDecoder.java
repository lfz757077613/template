package cn.laifuzhi.template.handler;

import cn.laifuzhi.template.model.tcp.BaseDTO;
import cn.laifuzhi.template.model.tcp.DataTypeEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.net.SocketAddress;
import java.util.Optional;

// 私有协议，前四字节是魔数，接着四个字节是数据长度，接着是一个完整数据包
// 完整数据包的第一个字节是类型，接着四个字节是dataId，接着两个字节是base数据长度，接着是base数据，最后是其他数据
@Slf4j
public class DataDecoder extends LengthFieldBasedFrameDecoder {
    public static final int MAGIC_NUMBER = 0xdeadbeaf;
    public static final int LENGTH_FIELD_OFFSET = Integer.BYTES;
    public static final int LENGTH_FIELD_LENGTH = Integer.BYTES;

    public DataDecoder(int maxBytes) {
        super(maxBytes, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf byteBuf = null;
        Integer dataId = null;
        Channel channel = ctx.channel();
        SocketAddress remoteAddress = channel.remoteAddress();
        try {
            //不是魔数开头直接拒绝访问
            if (in.readableBytes() >= Integer.BYTES && in.getInt(in.readerIndex()) != MAGIC_NUMBER) {
                log.info("magic number wrong number:{} remoteAddress:{}",
                        Integer.toHexString(in.getInt(in.readerIndex())), remoteAddress);
                ctx.close();
                return null;
            }
            byteBuf = (ByteBuf) super.decode(ctx, in);
            if (byteBuf == null) {
                return null;
            }
            // 跳过魔数和长度位
            byteBuf = byteBuf.skipBytes(LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH);
            byte dataType = byteBuf.getByte(byteBuf.readerIndex());
            dataId = byteBuf.getInt(byteBuf.readerIndex() + Byte.BYTES);
            Optional<DataTypeEnum> dataTypeOptional = DataTypeEnum.getByType(dataType);
            if (!dataTypeOptional.isPresent()) {
                log.error("data type not exist dataType:{} remoteAddress:{} dataId:{}", dataType, remoteAddress, dataId);
                return null;
            }
            Constructor<? extends BaseDTO> constructor = dataTypeOptional.get().getDataClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance().decode(byteBuf);
        } catch (Exception e) {
            log.error("decode error remoteAddress:{} dataId:{}", remoteAddress, dataId, e);
            return null;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }
}
