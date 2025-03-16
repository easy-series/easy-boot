package com.easy.web.controller;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基础控制器
 * 提供通用的控制器功能
 */
@RestController
public abstract class BaseController {

    @Autowired
    protected MessageSource messageSource;

    @Data
    protected static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> ok(T data) {
            Result<T> result = new Result<>();
            result.setCode(HttpStatus.OK.value());
            result.setMessage("操作成功");
            result.setData(data);
            return result;
        }

        public static <T> Result<T> error(String message) {
            Result<T> result = new Result<>();
            result.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            result.setMessage(message);
            return result;
        }
    }

    @Data
    protected static class PageResult<T> {
        private List<T> records;
        private long total;
        private long size;
        private long current;

        public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
            PageResult<T> result = new PageResult<>();
            result.setRecords(records);
            result.setTotal(total);
            result.setSize(size);
            result.setCurrent(current);
            return result;
        }
    }

    /**
     * 获取国际化消息
     */
    protected String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * 成功响应
     * 
     * @param data 响应数据
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<Result<T>> success(T data) {
        return ResponseEntity.ok(Result.ok(data));
    }

    /**
     * 成功响应（无数据）
     * 
     * @return ResponseEntity
     */
    protected ResponseEntity<Result<Void>> success() {
        return ResponseEntity.ok(Result.ok(null));
    }

    /**
     * 分页响应
     * 
     * @param records 记录列表
     * @param total   总记录数
     * @param size    每页大小
     * @param current 当前页
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<Result<PageResult<T>>> successPage(List<T> records, long total, long size,
            long current) {
        return ResponseEntity.ok(Result.ok(PageResult.of(records, total, size, current)));
    }

    /**
     * 错误响应
     * 
     * @param message 错误信息
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<Result<T>> error(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(message));
    }

    /**
     * 创建成功响应
     * 
     * @param data 响应数据
     * @return ResponseEntity
     */
    protected <T> ResponseEntity<Result<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.ok(data));
    }

    /**
     * 错误响应
     */
    protected <T> Result<T> error(Integer code, String message) {
        return Result.error(message);
    }
}