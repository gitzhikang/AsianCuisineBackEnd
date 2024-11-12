package com.asiancuisine.asiancuisine.interceptor;

import com.asiancuisine.asiancuisine.constant.JwtClaimsKeyConstant;
import com.asiancuisine.asiancuisine.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtTokenResetPasswordInterceptor implements HandlerInterceptor {

    @Value("${jwt.reset-password-secret-key}")
    private String resetPasswordSecretKey;


    @Value("${jwt.reset-password-token-name}")
    private String resetPasswordTokenName;

    /**
     * authenticate jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // judge whether the interceptor
        // intercepts the controller method
        // or other resources
        if (!(handler instanceof HandlerMethod)) {
            // if not dynamic method, pass
            return true;
        }

        // 1. receive token from the request header
        String authorizationHeader = request.getHeader(resetPasswordTokenName);

        // 2. if token is missing or does not start with "Bearer ", return 401
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        // extract the token
        String token = authorizationHeader.substring(7);

        // 3. authenticate the token
        try {
            Claims claims = JwtUtil.parseJWT(resetPasswordSecretKey, token);
            Long empId = Long.valueOf(claims.get(JwtClaimsKeyConstant.USER_ID).toString());
            // 3. pass
            return true;
        } catch (Exception ex) {
            // 4. not pass, return 401
            response.setStatus(401);
            return false;
        }
    }
}
