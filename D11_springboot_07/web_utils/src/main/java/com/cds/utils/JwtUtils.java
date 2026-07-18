package com.cds.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 令牌工具类
 * 用于生成和解析 JWT 令牌
 */
@Slf4j
public class JwtUtils {

    // 与测试类 JwtTest 中使用相同的密钥
    private static final String SECRET_STRING = "Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    // 令牌过期时间：1小时
    private static final long EXPIRATION_MILLIS = 3600000*10L;

    /**
     * 生成 JWT 令牌
     *
     * @param claims 自定义声明（如用户ID、用户名等）
     * @return JWT 令牌字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        String token = Jwts.builder()
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MILLIS))
                .signWith(KEY)
                .compact();
        log.info("JWT 令牌已生成, 过期时间: {} 毫秒后", EXPIRATION_MILLIS);
        return token;
    }

    /**
     * 解析 JWT 令牌
     *
     * @param token JWT 令牌字符串
     * @return 令牌中的声明信息；解析失败返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT 令牌解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证令牌是否有效
     *
     * @param token JWT 令牌字符串
     * @return true=有效, false=无效或过期
     */
    public static boolean validateToken(String token) {
        Claims claims = parseToken(token);
        return claims != null;
    }
}
