package cn.laifuzhi.template.utils;

import cn.laifuzhi.template.model.http.req.CommonContext;

import java.util.Objects;
import java.util.Optional;

public final class CommonThreadLocal {
    private static final ThreadLocal<CommonContext> ctx = new ThreadLocal<>();

    public static void put(CommonContext context) {
        Objects.requireNonNull(context);
        ctx.set(context);
    }

    public static Optional<CommonContext> get() {
        return Optional.ofNullable(ctx.get());
    }

    public static void remove() {
        ctx.remove();
    }

}
