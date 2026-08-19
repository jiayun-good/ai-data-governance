package com.mp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


public class JwtUtil {
    // 密钥
    private static final String SECRET = "my-secret-key-my-secret-key-my-secret-key-123456";

    // 过期时间：24小时
    private static final long EXPIRE_TIME = 30L * 60 * 60 * 1000;

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成 JWT
     */
    public static String generateToken(Long userId) {

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析并验证 JWT
     *
     * @param token JWT Token
     * @return 用户名
     */
    public static Long parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());

        } catch (Exception e) {
            return null;
        }
    }
}
