package com.nshj.mall.constant;

/**
 * 日志处理策略常量定义
 * <p>
 * <b>架构定位：</b>
 * 定义日志消费者端的 <b>微批处理 (Micro-batching)</b> 策略参数。
 * <p>
 * <b>设计意图：</b>
 * 为了防止高并发下对 Elasticsearch 发起过频的写入请求（导致 ES 线程池耗尽或 Segment Merge 压力过大），
 * 采用 <b>"空间换时间"</b> 策略：在内存中聚合一定数量或等待一定时间后，执行一次 Bulk Insert。
 *
 * @author nshj
 * @since 1.0.0
 */
public class LogConstants {

    /**
     * 批量刷盘阈值 (Volume Threshold)
     * <p>
     * <b>语义：</b> 内存缓冲区最大容量。
     * 当堆积的日志条数达到此数值时，立即触发 {@code flush} 操作写入 ES。
     */
    public static final int BATCH_SIZE = 100;

    /**
     * 时间刷盘阈值 (Time Threshold)
     * <p>
     * <b>语义：</b> 最大等待间隔 (单位：秒)。
     * 即使流量很低未达到 {@code BATCH_SIZE}，每隔 10 秒也会强制刷盘，确保日志数据的可见性延迟不超过该设定值。
     */
    public static final int FLUSH_INTERVAL = 10;

    /**
     * 请求上下文键：用户 ID
     * <p>
     * <b>作用域：</b> {@code HttpServletRequest.setAttribute()}
     * <p>
     * <b>业务场景：</b>
     * 在 {@code JwtAuthenticationTokenFilter} 解析 Token 成功后，将 UserId 放入 Request 域。
     * 后续的 {@code AuditLogAspect} 或 Controller 可直接提取，无需重复从 {@code SecurityContext} 解析，提升性能。
     */
    public static final String LOG_ATTR_USER_ID = "LOG_TEMP_USER_ID";

    /**
     * 请求上下文键：用户名称
     * <p>
     * <b>作用域：</b> {@code HttpServletRequest.setAttribute()}
     * <p>
     * <b>业务场景：</b>
     * 同上，用于在链路下游快速获取当前操作人的账号名称 (Username)，用于日志审计记录。
     */
    public static final String LOG_ATTR_USER_NAME = "LOG_TEMP_USER_NAME";

    /**
     * 私有构造器
     * <p>
     * 这是一个纯常量容器类，明确禁止实例化。
     */
    private LogConstants() {
    }
}