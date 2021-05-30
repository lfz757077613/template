package cn.laifuzhi.template.model.http.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public final class Resp<T> {
    private int code;
    private String message;
    private T data;

    private Resp(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> Resp<T> build(RespEnum respEnum) {
        return new Resp<>(respEnum.getCode(), respEnum.getMessage());
    }

    public static <T> Resp<T> build(RespEnum respEnum, T t) {
        return new Resp<>(respEnum.getCode(), respEnum.getMessage(), t);
    }
}
