package cn.laifuzhi.template.utils;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Constraint(validatedBy = AliyunRegionValidator.class)
public @interface AliyunRegion {

    String message() default "region not support";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
