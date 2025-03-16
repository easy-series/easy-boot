package com.easy.web.validate;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "easy.web.validation")
@Data
public class ValidationProperties {
    /**
     * 是否开启参数校验
     */
    private boolean enabled = true;

    /**
     * 是否使用快速失败模式
     */
    private boolean failFast = true;
}