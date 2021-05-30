package cn.laifuzhi.template.model.tcp;

import lombok.Getter;

@Getter
public class Ping extends BaseInfoReq {

    public Ping() {
        super(DataTypeEnum.PING);
    }

    @Override
    public DataTypeEnum dataType() {
        return DataTypeEnum.PING;
    }
}
