package com.easy.web.apilog;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * API 访问日志的 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ApiAccessLogApiImpl implements ApiAccessLogApi {

    @Override
    public void createApiAccessLog(ApiAccessLogCreateReqDTO createDTO) {
    }

}