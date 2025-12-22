package com.nshj.mall.constant;

/**
 * Redis 缓存键命名空间常量管理类
 * <p>
 * 集中定义系统中使用的 Redis Key 前缀与静态键名，确保缓存键命名的规范性与统一性。
 * <br>采用 "模块:业务:类型" 的分层命名规范（冒号分隔），不仅能有效防止不同业务模块间的 Key 冲突，
 * 还能在 Redis Desktop Manager 等可视化工具中实现目录层级的自动折叠与分组管理。
 *
 * @author nshj
 * @version 1.0.0
 */
public class RedisConstants {

    /**
     * 用户登录会话缓存前缀
     * <p>
     * <b>键结构规范：</b> {@code user:token:{token_uuid}}
     * <p>
     * <b>业务场景：</b>
     * 用于存储已登录用户的会话信息（如 UserDetail 对象），实现 JWT 的服务端有状态管理。
     * <ul>
     * <li><b>性能优化：</b> 将高频访问的用户基础信息缓存至内存，减少数据库 IO 压力。</li>
     * <li><b>安全控制：</b> 支持通过删除对应的 Redis Key 实现强制用户下线（踢出）功能。</li>
     * </ul>
     */
    public static final String USER_TOKEN_KEY = "user:token:";

    /**
     * 系统动态权限规则配置缓存 Key
     * <p>
     * <b>数据结构建议：</b> {@code Hash}
     * <ul>
     * <li><b>HashKey:</b> 接口请求路径 (如 /api/user/**)</li>
     * <li><b>HashValue:</b> 对应的权限标识或角色编码</li>
     * </ul>
     * <p>
     * <b>业务场景：</b>
     * 缓存全量的 "URL-权限" 映射规则。网关或拦截器在鉴权时直接读取此缓存，
     * 实现动态权限控制（Dynamic Authorization），无需频繁查询数据库中的权限配置表。
     */
    public static final String SYS_AUTH_RULES_KEY = "sys:auth:rules";

    /**
     * 私有构造器
     * <p>
     * 这是一个纯常量容器类，明确禁止实例化。
     */
    private RedisConstants() {
    }
}