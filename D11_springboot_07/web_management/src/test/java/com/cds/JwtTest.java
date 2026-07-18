package com.cds;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtTest {

    @Test
    public void testGenerateJWT() {
        // 生成 HMAC-SHA256 密钥（至少 256 位 / 32 字节）
        SecretKey key = Keys.hmacShaKeyFor("Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==".getBytes());

        Map<String, Object> claims = Map.of("id", "1", "username", "陈德圣");

        // 生成 JWT（jjwt 0.12.x 新 API）
        String token = Jwts.builder()
                .claims(claims)                                          // 自定义声明（替代 addClaims）
                .expiration(new Date(System.currentTimeMillis() + 3600000*100)) // 1小时过期（替代 setExpiration）
                .signWith(key)                                           // 签名（传入 Key 对象）
                .compact();

        System.out.println("生成的 Token: " + token);
    }

    @Test
    public void testParseJWT() {

        SecretKey key = Keys.hmacShaKeyFor("Y2RzY2RzY2RzY2RzY2RzY2RzY2RzMTIzNA==".getBytes());
        String token = "eyJhbGciOiJIUzI1NiJ9" +
                ".eyJpZCI6IjEiLCJ1c2VybmFtZSI6IumZiOW-t-WcoyIsImV4cCI6MTc4NDQ2NjA4Nn0" +
                ".tckYqMd569dtWRdPVtluZbmNDce_JOZcS9_S5W7wt8E";
        // 解析 JWT
        Claims claim = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getBody();

        System.out.println("解析出的内容: " + claim);
    }
}

