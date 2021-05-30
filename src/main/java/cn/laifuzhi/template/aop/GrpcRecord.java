package cn.laifuzhi.template.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * grpc方法参数、响应记录、注入traceId
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcRecord {

    /**
     * 是否记录请求参数
     */
    boolean recordReq() default true;

    /**
     * 是否记录请求结果
     */
    boolean recordResp() default false;

}
