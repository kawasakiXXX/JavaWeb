package com.cds.javaweb.web_01.config;

import com.cds.javaweb.web_01.filter.TokenFilter;
import com.cds.javaweb.web_01.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 注册拦截器和过滤器
 *
 * 执行顺序：
 *   1. TokenFilter（过滤器）—— 验证 JWT 令牌（先执行）
 *   2. LoginInterceptor（拦截器）—— 处理登录请求（后执行）
 *
 * 请求链路：
 *   客户端 → TokenFilter → DispatcherServlet → LoginInterceptor → Controller
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    /**
     * 注册登录拦截器
     * 拦截 /login 登录请求，验证用户名密码并生成令牌
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/login")    // 只拦截登录请求
                .order(1);
    }

    /**
     * 注册 Token 验证过滤器
     * 过滤所有请求，验证 JWT 令牌（/login 路径在白名单中会被放行）
     */
    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilterRegistration() {
        FilterRegistrationBean<TokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TokenFilter());
        registration.addUrlPatterns("/*");   // 过滤所有请求
        registration.setOrder(1);            // 过滤器优先级（数字越小越先执行）
        registration.setName("tokenFilter");
        return registration;
    }
}
