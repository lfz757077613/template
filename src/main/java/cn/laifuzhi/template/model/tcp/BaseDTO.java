package cn.laifuzhi.template.model.tcp;

import io.netty.buffer.ByteBuf;

public interface BaseDTO {
    BaseDTO decode(ByteBuf byteBuf);

    ByteBuf encode(ByteBuf byteBuf);

    DataTypeEnum dataType();
}
