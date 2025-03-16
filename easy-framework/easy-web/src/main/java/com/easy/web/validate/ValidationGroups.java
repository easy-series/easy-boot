package com.easy.web.validate;

/**
 * 校验分组
 */
public interface ValidationGroups {
    
    /**
     * 创建操作
     */
    interface Create {}
    
    /**
     * 更新操作
     */
    interface Update {}
    
    /**
     * 删除操作
     */
    interface Delete {}
    
    /**
     * 查询操作
     */
    interface Query {}
    
    /**
     * 导出操作
     */
    interface Export {}
    
    /**
     * 导入操作
     */
    interface Import {}
}