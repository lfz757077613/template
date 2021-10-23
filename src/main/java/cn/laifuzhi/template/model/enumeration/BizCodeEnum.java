package cn.laifuzhi.template.model.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizCodeEnum {
    OK(0, "ok"),
    ILLEGAL_PARAM(1, "illegal param"),
    TIMEOUT(2, "timeout"),
    SYSTEM_ERROR(3, "system error"),
    UNKNOWN_ERROR(4, "unknown error"),

    NO_LOGIN(1001, "non login"),
    NO_ROLE(1002, "non role"),
    NO_PERM(1003, "non perm"),
    USER_NOT_EXISTED(1004, "user not existed"),
    ORDER_DUPLICATE(1005, "order duplicate"),
    ORDER_NOT_EXISTED(1006, "order not exist"),
    ORDER_STATE_WRONG(1007, "order state wrong"),
    ;
    private final int code;
    private final String message;
}
