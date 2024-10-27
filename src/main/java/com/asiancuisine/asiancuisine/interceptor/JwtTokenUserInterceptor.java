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
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Value("jwt.user-secret-key")
    private String userSecretKey;


    @Value("jwt.user-token-name")
    private String userTokenName;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(userTokenName);

        //2、校验令牌
        try {
            log.info("jwt verification:{}", token);
            Claims claims = JwtUtil.parseJWT(userSecretKey, token);
            Long empId = Long.valueOf(claims.get(JwtClaimsKeyConstant.USER_ID).toString());
            log.info("userId:{}", empId);
            BaseContext.setCurrentId(empId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}