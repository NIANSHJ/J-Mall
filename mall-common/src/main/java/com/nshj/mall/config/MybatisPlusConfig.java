package com.nshj.mall.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 框架核心配置类
 * <p>
 * 负责集中管理和注册 MyBatis-Plus 的扩展插件体系。
 * 通过定义拦截器链（Interceptor Chain），集成如分页插件、乐观锁插件、防止全表更新插件等核心功能。
 * 确保数据库操作能够利用 MP 框架提供的高级特性，提升开发效率与运行性能。
 *
 * @author nshj
 * @version 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 核心拦截器容器
     * <p>
     * 创建并配置 {@link MybatisPlusInterceptor} Bean 实例，作为后续所有内部插件（InnerInterceptor）的载体。
     * <p>
     * <b>当前配置策略：</b>
     * <ul>
     * <li>集成 {@link PaginationInnerInterceptor}：开启数据库层面的物理分页功能（SQL Limit）。
     * 若未配置此插件，MyBatis-Plus 的分页查询将退化为内存分页，可能导致严重的内存溢出或性能问题。</li>
     * <li>指定数据库方言：明确设置 {@link DbType#MYSQL}，避免框架运行时自动推断数据库类型，从而提升 SQL 解析效率。</li>
     * </ul>
     *
     * @return 包含已装配内部插件的拦截器链对象
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页内部拦截器
        // 显式指定 DbType.MYSQL 可跳过 JDBC URL 解析步骤，略微提升启动速度和 SQL 解析效率
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}