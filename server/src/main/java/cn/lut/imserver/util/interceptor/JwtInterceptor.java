package cn.lut.imserver.util.interceptor;

import cn.lut.imserver.util.JWTUtil;
import cn.lut.imserver.util.UserContext;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



/**
 * JWT拦截器，用于验证请求中的token
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        response.setContentType("application/json;charset=utf-8");

        // 如果请求方法是OPTIONS，直接返回200 OK
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return false;
        }

        // 如果没有token，直接返回401未授权
        if (token == null || token.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", 401);
            jsonObject.put("message", "token is null");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(jsonObject.toJSONString());
            return false;
        }

        // 验证token
        Jws<Claims> jws = jwtUtil.parseToken(token);

        // 如果验证失败，返回401未授权
        if (jws == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", 401);
            jsonObject.put("message", "token is unauthorized");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(jsonObject.toJSONString());
            return false;
        }

        // 将用户信息存入请求属性中，供后续处理使用
        request.setAttribute("uid", jws.getPayload().get("uid"));
        request.setAttribute("username", jws.getPayload().get("username"));

        Object uid = jws.getPayload().get("uid");
        if (uid instanceof String) {
            UserContext.setUid(Long.parseLong((String) uid));
        } else {
            log.error("Unsupported class of uid");
            log.error(uid.getClass().toString());
        }
        Object username = jws.getPayload().get("username");
        if (username instanceof String) {
            UserContext.setUsername((String) username);
        } else {
            log.error("Unsupported class of username");
        }
        return true;
    }
}

