package cn.laifuzhi.template.model.tcp;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseInfoResp implements BaseDTO {
    private static final int MAX_RESP_FROM_LENGTH = 64;
    private byte dataType;
    private int dataId;
    private byte respType;

    BaseInfoResp(BaseInfoReq req, DataTypeEnum dataType, RespTypeEnum respType) {
        this.dataType = dataType.getType();
        this.dataId = req.getDataId();
        this.respType = respType.getType();
    }

    @Override
    public BaseInfoResp decode(ByteBuf byteBuf) {
        this.dataType = byteBuf.readByte();
        this.dataId = byteBuf.readInt();
        byteBuf = byteBuf.readSlice(byteBuf.readShort());
        this.respType = byteBuf.readByte();
        return this;
    }

    @Override
    public ByteBuf encode(ByteBuf byteBuf) {
        byteBuf = byteBuf.writeByte(this.dataType);
        byteBuf = byteBuf.writeInt(this.dataId);
        byteBuf = byteBuf.writeByte(this.respType);
        return byteBuf;
    }
}
