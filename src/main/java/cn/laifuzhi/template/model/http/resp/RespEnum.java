package cn.laifuzhi.template.model.http.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RespEnum {
    OK(0, "ok"),
    ILLEGAL_PARAM(1, "illegal param"),
    UNKNOWN_ERROR(2, "unknown error"),

    NO_LOGIN(1001, "non login"),
    NO_PERM(1002, "non perm"),
    ;
    private final int code;
    private final String message;
}
