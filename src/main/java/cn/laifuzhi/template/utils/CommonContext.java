package cn.laifuzhi.template.utils;

import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Setter
@Getter
public final class CommonContext {
    private static final ThreadLocal<CommonContext> ctx = new ThreadLocal<>();

    private transient HttpServletRequest servletReq;
    private transient HttpServletResponse servletResp;
    private String uid;

    public static void put(CommonContext context) {
        Objects.requireNonNull(context);
        ctx.set(context);
    }

    public static CommonContext get() {
        return Objects.requireNonNull(ctx.get());
    }

    public static void remove() {
        ctx.remove();
    }
}
