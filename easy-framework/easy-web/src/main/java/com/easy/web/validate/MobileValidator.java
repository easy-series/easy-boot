package com.easy.web.validate;

import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 手机号校验器
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public void initialize(Mobile annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果为空，直接通过校验
        if (!StringUtils.hasText(value)) {
            return true;
        }
        // 校验手机号格式
        return PATTERN.matcher(value).matches();
    }
}