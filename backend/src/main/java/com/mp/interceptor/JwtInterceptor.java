package com.mp.interceptor;

import com.mp.context.UserContext;
import com.mp.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        // 1. 获取请求头
        String authorization = request.getHeader("Authorization");

        // 2. 判断 Token 是否存在
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }

        // 3. 去掉 Bearer
        String token = authorization.substring(7);

        // 4. 校验 Token-解析 JWT
        Long userId = JwtUtil.parseToken(token);

        // 5. Token 校验失败
        if (userId == null) {
            throw new RuntimeException("Token无效或已过期");
        }

        // 6. 放入 ThreadLocal
        UserContext.setUserId(userId);

        // 7. 放行
        return true;
    }
}
