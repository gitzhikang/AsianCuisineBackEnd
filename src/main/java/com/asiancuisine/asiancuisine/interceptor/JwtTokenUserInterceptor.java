package com.asiancuisine.asiancuisine.interceptor;

import com.asiancuisine.asiancuisine.constant.JwtClaimsKeyConstant;
import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt token authentication interceptor
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Value("jwt.user-secret-key")
    private String userSecretKey;


    @Value("jwt.user-token-name")
    private String userTokenName;

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
        String token = request.getHeader(userTokenName);

        // 2. authenticate the token
        try {
            log.info("jwt verification:{}", token);
            Claims claims = JwtUtil.parseJWT(userSecretKey, token);
            Long empId = Long.valueOf(claims.get(JwtClaimsKeyConstant.USER_ID).toString());
            log.info("userId:{}", empId);
            BaseContext.setCurrentId(empId);
            // 3. pass
            return true;
        } catch (Exception ex) {
            // 4. not pass, return 401
            response.setStatus(401);
            return false;
        }
    }
}
