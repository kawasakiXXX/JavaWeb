package com.cds.filter;

import com.cds.pojo.Result;
import com.cds.utils.CurrentHolder;
import com.cds.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Token 验证过滤器
 * 验证请求中携带的 JWT 登录令牌，确保未登录用户无法访问受保护接口
 */
@Slf4j
public class TokenFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 不需要 token 验证的路径（白名单） */
    private static final String[] EXCLUDE_PATHS = {
            "/login",
            "/error"   // Spring Boot 默认错误页面
    };

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestURI = request.getRequestURI();// 请求路径
        String method = request.getMethod();// 请求方法

        log.info("=== Token 过滤器：拦截请求 {} {} ===", method, requestURI);

        // 白名单路径直接放行（登录请求不需要 token）
        if (isExcluded(requestURI)) {
            log.info("请求路径 {} 在白名单中，放行", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 获取请求头中的 token
        String token = request.getHeader("token");
        if (token == null) {
            log.warn("请求格式不正确, URI={}", requestURI);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录，请先进行登录");
            return;
        }

        // 验证 token
        Claims claims = JwtUtils.parseToken(token);



        if (claims == null) {
            log.warn("Token 无效或已过期, URI={}", requestURI);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "令牌无效或已过期，请重新登录");
            return;
        }

        // token 有效，将用户信息存入 request 属性，供后续使用
        String userId = claims.get("id", String.class);
        String username = claims.get("username", String.class);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        // 将当前用户ID存入 ThreadLocal，方便后续使用
        CurrentHolder.setCurrentUserId(Integer.valueOf(userId));
        log.info("当前登录ID: {}，将其存入ThreadLocal", userId);


        // 放行，finally 确保 ThreadLocal 一定被清理
        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentHolder.remove();
        }

    }

    /**
     * 判断请求路径是否在白名单中
     */
    private boolean isExcluded(String requestURI) {
        for (String path : EXCLUDE_PATHS) {
            if (requestURI.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 向客户端返回 JSON 格式的错误信息
     */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(message)));
    }
}
