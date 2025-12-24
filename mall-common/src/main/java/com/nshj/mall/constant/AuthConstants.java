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
     * Redis 中存储的权限规则 TTL。设置为 24 小时，配合定时任务刷新，
     * 既保证了数据新鲜度，又防止了 Redis 内存长期占用。
     */
    public static final int AUTH_EXPIRATION = 24;

    /**
     * 权限自动刷新 Cron 表达式
     * <p>
     * <b>策略：</b> 每小时执行一次 (0分0秒触发)。
     * <b>目的：</b> 作为 "最终一致性" 的兜底方案，修正因广播消息丢失或 Redis 逐出导致的数据不一致。
     */
    public static final String AUTH_FLUSH_CRON = "0 0 * * * ?";

    /**
     * 用户初始化/重置默认密码
     * <p>
     * <b>值：</b> "123456"
     * <p>
     * <b><font color="red">安全警告：</font></b>
     * 1. 仅用于 <b>管理员创建新用户</b> 或 <b>重置密码</b> 时的临时凭证。
     * 2. 系统应当实施 <b>"首次登录强制改密"</b> 策略，防止弱密码长期存在带来的被爆破风险。
     */
    public static final String DEFAULT_PASSWORD = "123456";
    
    /**
     * 复合键分隔符 (Composite Key Separator)
     * <p>
     * <b>作用：</b> 用于拼接 HTTP Method 与 URL Path，构建唯一的权限规则 Key。
     * <br>
     * <b>格式示例：</b> {@code POST:api/user/add}
     */
    public static final String KEY_SEPARATOR = ":";

    /**
     * 全动词通配符 (Universal Method Wildcard)
     * <p>
     * <b>业务语义：</b> 当数据库规则未指定具体 Request Method 时，使用此标识。
     * <br>
     * <b>匹配逻辑：</b> 标识该 URL 路径下的所有操作 (GET, POST, PUT, DELETE) 均适用同一套权限规则。
     */
    public static final String METHOD_ALL = "ALL";

    /**
     * 私有构造器
     * <p>
     * 这是一个纯常量容器类，明确禁止实例化。
     */
    private AuthConstants() {
    }
}