package cn.laifuzhi.template.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 处理权限认证
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpBiz {
    boolean recordReq() default false;

    boolean recordResp() default false;

    boolean needLogin() default false;

    String perm() default "";

    String role() default "";

}
