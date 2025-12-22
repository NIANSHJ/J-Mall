package com.nshj.mall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层基础设施配置
 * <p>
 * 全局定制 Spring MVC 的核心行为。
 * 核心职能是打破浏览器同源策略 (Same-Origin Policy) 的限制，建立标准化的跨域资源共享 (CORS) 规范，
 * 确保前后端分离架构下的 HTTP 通信通畅。
 *
 * @author nshj
 * @since 1.0.0
 */
@Configuration
public class GlobalMvcConfig implements WebMvcConfigurer {

    /**
     * 定义全局跨域资源共享策略 (CORS Policy)
     * <p>
     * <b>配置策略详解：</b>
     * <ol>
     * <li><b>路径覆盖：</b> {@code /**} - 匹配系统内所有 API 接口。</li>
     * <li><b>源域白名单：</b> {@code allowedOriginPatterns("*")} - 允许任意域名访问（开发/测试环境宽松模式）。
     * <br>注意：配合 {@code allowCredentials(true)} 使用时，Spring 会自动将请求头中的 Origin 回填至响应头，而非直接返回通配符 *。</li>
     * <li><b>HTTP 动词：</b> 覆盖标准 RESTful 方法 (GET, POST, PUT, DELETE) 及预检方法 (OPTIONS)。</li>
     * <li><b>凭证传输：</b> {@code allowCredentials(true)} - 允许前端携带 Cookie 或 Authorization 头。</li>
     * <li><b>预检缓存：</b> {@code maxAge(3600)} - 浏览器缓存 OPTIONS 预检请求结果 1 小时，减少非必要网络交互。</li>
     * </ol>
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Spring Boot 2.4+ 必须使用 allowedOriginPatterns 以兼容 allowCredentials
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}