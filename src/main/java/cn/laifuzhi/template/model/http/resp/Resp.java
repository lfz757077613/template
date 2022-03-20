package cn.laifuzhi.template.model.http.resp;

import cn.laifuzhi.template.model.enumeration.BizCodeEnum;
import lombok.Getter;
import org.slf4j.MDC;

import static cn.laifuzhi.template.utils.Const.MDC_TRACE_ID;

@Getter
public final class Resp<T> {
    private final String requestId;
    private final int code;
    private final String message;
    private T data;


    private Resp(BizCodeEnum respEnum, String message, T data) {
        this.requestId = MDC.get(MDC_TRACE_ID);
        this.code = respEnum.getCode();
        this.message = message;
        this.data = data;
    }


    public static <K> Resp<K> fail(BizCodeEnum respEnum, String message) {
        return new Resp<>(respEnum, message != null ? message : respEnum.getMessage(), null);
    }

    public static <K> Resp<K> fail(String message) {
        return fail(BizCodeEnum.SYSTEM_ERROR, message);
    }

    public static <K> Resp<K> paramError(String message) {
        return fail(BizCodeEnum.ILLEGAL_PARAM, message);
    }

    public static <K> Resp<K> ok(BizCodeEnum respEnum, K k, String message) {
        return new Resp<>(respEnum, message != null ? message : respEnum.getMessage(), k);
    }

    public static <K> Resp<K> ok(K k, String message) {
        return ok(BizCodeEnum.OK, k, message);
    }

    public static <K> Resp<K> ok(K k) {
        return ok(k, null);
    }

    public static <K> Resp<K> ok() {
        return ok(null);
    }
}
