package cn.laifuzhi.template.utils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class AliyunRegionValidator implements ConstraintValidator<AliyunRegion, String> {
    @Override
    public boolean isValid(String regionId, ConstraintValidatorContext context) {
        return true;
    }
}
