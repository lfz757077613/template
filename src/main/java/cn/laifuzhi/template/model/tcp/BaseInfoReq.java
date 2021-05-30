package cn.laifuzhi.template.model.tcp;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseInfoReq implements BaseDTO {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();
    protected byte dataType;
    protected int dataId;

    BaseInfoReq(DataTypeEnum dataType) {
        this.dataType = dataType.getType();
        this.dataId = ID_GENERATOR.incrementAndGet();
    }

    @Override
    public BaseInfoReq decode(ByteBuf byteBuf) {
        this.dataType = byteBuf.readByte();
        this.dataId = byteBuf.readInt();
        return this;
    }

    @Override
    public ByteBuf encode(ByteBuf byteBuf) {
        byteBuf = byteBuf.writeByte(this.dataType);
        byteBuf = byteBuf.writeInt(this.dataId);
        return byteBuf;
    }
}
