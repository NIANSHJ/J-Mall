package com.nshj.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 基础配置类
 * <p>
 * 用于自定义 Spring Data Redis 的核心组件配置，重点对 {@link RedisTemplate} 的序列化机制进行重构。
 * 旨在解决默认 JDK 序列化导致的二进制乱码问题，提升 Redis 中数据的可读性与跨平台兼容性。
 *
 * @author nshj
 * @version 1.0.0
 */
@Configuration
public class RedisConfig {

    /**
     * 实例化并配置自定义序列化策略的 RedisTemplate
     * <p>
     * 该 Bean 将覆盖 Spring Boot 默认提供的 RedisTemplate，实现了 Key-Value 的分离序列化策略：
     * <ul>
     * <li><b>键 (Key) 处理：</b> 使用 {@link StringRedisSerializer}，确保在 Redis 客户端工具中键名以明文显示，便于运维调试。</li>
     * <li><b>值 (Value) 处理：</b> 使用 {@link GenericJackson2JsonRedisSerializer}，将 Java 对象转换为 JSON 格式存储。
     * 该序列化器会自动记录对象的类全限定名 (@class)，从而支持复杂对象的自动反序列化。</li>
     * </ul>
     *
     * @param connectionFactory 由 Spring 容器注入的 Redis 连接工厂，用于建立底层连接
     * @return 配置完毕的 RedisTemplate 实例，支持 String 类型的 Key 和 Object 类型的 Value
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置底层的连接工厂
        template.setConnectionFactory(connectionFactory);

        // 初始化序列化器：JSON 序列化器 (处理对象) 与 String 序列化器 (处理键)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 1. 设置 Key 的序列化规则：直接使用 String，避免二进制乱码
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 2. 设置 Value 的序列化规则：使用 JSON 格式存储，支持对象自动转换
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 初始化 RedisTemplate 的参数设置，确保所有属性配置生效
        template.afterPropertiesSet();
        return template;
    }
}