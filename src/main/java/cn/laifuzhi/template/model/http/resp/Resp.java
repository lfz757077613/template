package cn.laifuzhi.template.model.http.resp;

import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import lombok.Getter;
import org.slf4j.MDC;

import static cn.laifuzhi.template.utils.Const.MDC_TRACE_ID;

@Getter
public final class Resp<T> {
    private final String traceId;
    private final int code;
    private final String message;
    private T data;

    private Resp(int code, String message) {
        this.traceId = MDC.get(MDC_TRACE_ID);
        this.code = code;
        this.message = message;
    }

    private Resp(int code, String message, T data) {
        this.traceId = MDC.get(MDC_TRACE_ID);
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Resp<Void> build(BizCodeEnum bizCodeEnum) {
        return new Resp<>(bizCodeEnum.getCode(), bizCodeEnum.getMessage());
    }

    public static Resp<Void> build(BizCodeEnum bizCodeEnum, String message) {
        return new Resp<>(bizCodeEnum.getCode(), message);
    }

    public static <K> Resp<K> build(BizCodeEnum bizCodeEnum, K k) {
        return new Resp<>(bizCodeEnum.getCode(), bizCodeEnum.getMessage(), k);
    }

    public static <K> Resp<K> build(BizCodeEnum bizCodeEnum, String message, K k) {
        return new Resp<>(bizCodeEnum.getCode(), message, k);
    }
}
