package cn.laifuzhi.template.model.tcp;

import lombok.Getter;

@Getter
public class Pong extends BaseInfoResp {

    public Pong(Ping ping) {
        super(ping, DataTypeEnum.PONG, RespTypeEnum.OK);
    }

    @Override
    public DataTypeEnum dataType() {
        return DataTypeEnum.PONG;
    }
}
