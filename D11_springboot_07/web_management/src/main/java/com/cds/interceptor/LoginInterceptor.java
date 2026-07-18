package com.cds.interceptor;

import com.cds.pojo.LoginRequest;
import com.cds.pojo.Result;
import com.cds.pojo.User;
import com.cds.services.UserServices;
import com.cds.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
import java.util.Map;

/**
 * 登录拦截器
 * 在过滤器(TokenFilter)放行后、DispatcherServlet 调度之后，
 * 拦截 /login 登录请求，验证用户名密码并生成 JWT 令牌
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private UserServices userServices;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截 /login 的 POST 请求（登录操作）
        if ("/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            log.info("=== 登录拦截器：开始处理登录请求 ===");

            // 读取请求体中的 JSON 数据
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            // 反序列化为 LoginRequest
            LoginRequest loginRequest = objectMapper.readValue(body.toString(), LoginRequest.class);
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            log.info("登录请求: username={}", username);

            // 验证用户名密码
            User user = userServices.login(username, password);
            if (user == null) {
                log.warn("登录失败: 用户名或密码错误, username={}", username);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error("用户名或密码错误")));
                return false; // 拦截，不继续执行
            }

            // 生成 JWT 令牌（按测试类 JwtTest 的格式）
            Map<String, Object> claims = Map.of("id", user.getId().toString(), "username", user.getUsername());
            String token = JwtUtils.generateToken(claims);

            log.info("登录成功: username={}, token已生成", username);

            // 将令牌放入响应返回给前端
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.success(Map.of("token", token, "username", user.getUsername()))));
            return false; // 拦截，响应已在拦截器中完成
        }

        // 非登录请求，放行
        return true;
    }
}
