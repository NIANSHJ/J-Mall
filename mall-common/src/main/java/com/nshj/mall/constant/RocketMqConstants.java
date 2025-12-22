package com.nshj.mall.constant;

/**
 * RocketMQ 消息中间件拓扑定义 (Message Queue Topology)
 * <p>
 * <b>架构定位：</b>
 * 位于公共域 (Common Domain)，集中管理系统内所有的 Topic (主题) 与 Tag (标签) 定义。
 * <p>
 * <b>设计规约：</b>
 * 1. <b>消除硬编码：</b> 避免在生产者和消费者代码中散落魔法字符串，确保契约统一。
 * 2. <b>运维友好：</b> 规范的命名有助于在 RocketMQ Dashboard 中进行监控聚合与链路追踪。
 *
 * @author nshj
 * @since 1.0.0
 */
public class RocketMqConstants {

    /**
     * 系统级广播事件主题
     * <p>
     * <b>业务语义：</b>
     * 用于通知集群内所有节点执行状态同步或配置重载。
     * <p>
     * <b>关键配置：</b>
     * <font color="red">Consumer 必须配置为 BROADCASTING (广播模式)。</font>
     * <p>
     * <b>典型场景：</b>
     * 当 RBAC 权限（角色/菜单）发生变更时，生产端发送此消息，
     * 所有微服务节点消费消息并立即清理本地缓存 (Local Cache)，确保权限校验的数据一致性。
     */
    public static final String TOPIC_SYS_BROADCAST = "sys-broadcast-topic:";
    public static final String TAG_AUTH = "auth";

    /**
     * 操作日志流水主题
     * <p>
     * <b>业务语义：</b>
     * 承载高吞吐量的业务审计日志流。
     * <p>
     * <b>处理策略：</b>
     * 采用 {@code Clustering} (集群模式) 消费，通常由独立的数据清洗服务消费并写入 OLAP 存储 (如 ClickHouse/Elasticsearch)。
     */
    public static final String TOPIC_MALL_LOG = "sys-log-topic";
    public static final String TAG_LOG_AUDIT = "audit";

    /**
     * 私有构造器
     * <p>
     * 静态常量容器，禁止实例化。
     */
    private RocketMqConstants() {
    }
}