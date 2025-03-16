/**
 * Web 框架，全局异常、API 日志等
 * <p>
 * OncePerRequestFilter：
 * 属于 Servlet Filter 机制，是 Servlet 规范的一部分
 * 在 Servlet 容器级别工作（如 Tomcat）
 * 位于 Spring 应用上下文之外
 * <p>
 * HandlerInterceptor：
 * 属于 Spring MVC 框架内部的拦截器机制
 * 在 Spring 应用上下文中工作
 * 由 DispatcherServlet 管理和调用
 */
package com.easy.web;
