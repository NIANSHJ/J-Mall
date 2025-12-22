package com.nshj.mall.constant;

/**
 * 权限安全相关常量定义
 * <p>
 * 集中管理鉴权逻辑中的 <b>时间窗口</b> 与 <b>调度策略</b>，确保系统安全策略的一致性。
 *
 * @author nshj
 * @since 1.0.0
 */
public class AuthConstants {

    /**
     * 权限规则缓存有效期 (单位：小时)
     * <p>
     * Redis 中存储的权限规则 TTL。设置为 1 小时，配合定时任务刷新，
     * 既保证了数据新鲜度，又防止了 Redis 内存长期占用。
     */
    public static final int AUTH_EXPIRATION = 1;

    /**
     * 权限自动刷新 Cron 表达式
     * <p>
     * <b>策略：</b> 每小时执行一次 (0分0秒触发)。
     * <b>目的：</b> 作为 "最终一致性" 的兜底方案，修正因广播消息丢失或 Redis 逐出导致的数据不一致。
     */
    public static final String AUTH_FLUSH_CRON = "0 0 * * * ?";

    /**
     * 私有构造器
     * <p>
     * 这是一个纯常量容器类，明确禁止实例化。
     */
    private AuthConstants() {
    }
}