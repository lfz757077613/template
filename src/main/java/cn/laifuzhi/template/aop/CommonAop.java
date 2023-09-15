package cn.laifuzhi.template.aop;

import com.alibaba.messaging.ops2.model.enumeration.RecordOperateEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommonAop {
    boolean recordReqLog() default true;

    boolean recordRespLog() default false;

    RecordOperateEnum recordHistory() default RecordOperateEnum.NONE;
}
