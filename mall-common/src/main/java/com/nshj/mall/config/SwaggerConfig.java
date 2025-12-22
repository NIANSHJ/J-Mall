package com.nshj.mall.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 接口文档全局配置类
 * <p>
 * 基于 SpringDoc 框架，负责定义和初始化项目 API 文档的元数据信息。
 * 通过注册 {@link OpenAPI} Bean，集中管理文档标题、业务描述、接口版本及维护团队等核心信息。
 * 该配置将直接影响 Swagger UI 或 Knife4j 等可视化界面的首页展示内容。
 *
 * @author nshj
 * @version 1.0.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * 构建全局 OpenAPI 元数据实例
     * <p>
     * 初始化 API 文档的基础概览信息 (Info)，具体配置包含：
     * <ul>
     * <li><b>标题 (Title)：</b> 定义文档系统的显示名称。</li>
     * <li><b>描述 (Description)：</b> 阐述项目的技术架构背景或业务范围。</li>
     * <li><b>版本 (Version)：</b> 标记当前接口文档的迭代版本号。</li>
     * <li><b>联系人 (Contact)：</b> 提供开发团队的联系方式以便于对接反馈。</li>
     * </ul>
     * 此 Bean 将被 SpringDoc 上下文自动扫描，作为生成 OpenAPI 规范文件（JSON/YAML）的基础。
     *
     * @return 包含完整元数据信息的 OpenAPI 实例对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("J-Mall 后端接口文档")
                        .description("基于 Spring Boot 3 开发的基础电商架构")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NSHJ Team")
                                .email("dev@nshj.com")));
    }
}