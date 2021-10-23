package cn.laifuzhi.template.model.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStateEnum {
    CREATED(0, "created"),
    PAID(1, "paid"),
    ;
    private final int state;
    private final String desc;
}
