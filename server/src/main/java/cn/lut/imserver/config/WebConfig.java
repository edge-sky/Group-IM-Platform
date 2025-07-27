package cn.lut.imserver.config;

import cn.lut.imserver.util.interceptor.JwtInterceptor;
import cn.lut.imserver.util.interceptor.PermissionCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，用于注册拦截器和配置CORS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private PermissionCheckInterceptor permissionCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加JWT拦截器，并配置拦截路径
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除不需要拦截的路径
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/error"
                );

        // 添加权限检查拦截器，并配置拦截路径
        registry.addInterceptor(permissionCheckInterceptor)
                .addPathPatterns(
                        "/conversation/**",
                        "/*/conversation/**",  // 匹配 /{id}/conversation/**
                        "/file/**",
                        "/*/file/**",          // 匹配 /{id}/file/**
                        "/folder/**",
                        "/*/folder/**"
                )
                .excludePathPatterns(
                        "/conversation/create",
                        "/conversation/join",
                        "/user/verify",
                        "/conversation/list"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有路径生效
                .allowedOriginPatterns("*") // 允许所有来源的请求，生产环境中建议替换为你的前端应用的具体源，例如 "http://localhost:5173"
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*") // 允许所有的请求头，包括 Authorization
                .allowCredentials(true) // 是否允许发送Cookie，对于携带token的认证通常需要
                .maxAge(3600); // 预检请求的有效期，单位秒
    }
}
