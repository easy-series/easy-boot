package com.easy.web.apilog;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * API 访问日志的 API 接口
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ApiErrorLogApiImpl implements ApiErrorLogApi {

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
    }

}
