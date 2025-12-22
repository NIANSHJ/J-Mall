package com.nshj.mall.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志文档模型 (Audit Log Document)
 * <p>
 * <b>架构定位：</b>
 * 映射 Elasticsearch 中的 {@code sys_audit_log} 索引。
 * 定义了日志数据的存储结构（Schema），直接决定了 Kibana 仪表盘的查询效率与聚合能力。
 * <p>
 * <b>字段设计原则：</b>
 * <ul>
 * <li><b>精确检索 (Keyword):</b> 用于枚举值（如模块、状态码）或标识符（如用户名），不分词，支持聚合统计 (Aggregation)。</li>
 * <li><b>全文检索 (Text):</b> 用于长文本（如参数、错误堆栈），支持倒排索引分词，用于模糊搜索。</li>
 * <li><b>时序数据 (Date):</b> 用于时间范围筛选，是 ELK 日志分析的时间轴基准。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@Document(indexName = "sys_audit_log")
public class AuditLog implements Serializable {

    /**
     * 分布式追踪 ID (Trace ID)
     * <p>
     * <b>主键策略：</b>
     * 直接复用链路追踪 ID 作为 ES 文档的 {@code _id}。
     * <br>
     * <b>设计意图：</b>
     * 1. <b>幂等性保障：</b> 即使 MQ 消息重复消费，相同的 Trace ID 只会覆盖旧文档，不会产生重复数据。
     * 2. <b>全链路关联：</b> 可凭此 ID 在 SkyWalking/Zipkin 等系统中关联查询应用性能指标。
     */
    @Id
    private String traceId;

    /**
     * 事件发生时间
     * <p>
     * <b>映射类型：</b> Date
     * <br>
     * <b>核心作用：</b> Kibana 界面中 Time Filter (右上角时间选择器) 的依据字段。
     */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createTime;

    /**
     * 操作人 ID
     * <p>
     * <b>映射类型：</b> Long
     * <br>
     * <b>作用：</b> 即使用户名变更，ID 依然保持恒定，用于精确审计。
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 操作人账号
     * <p>
     * <b>映射类型：</b> Keyword (不分词)
     * <br>
     * <b>场景：</b> 适用于 "LowCardinality" (低基数) 场景。例如：在 Kibana 中按 "User" 进行 Group By 统计 Top 10 活跃用户。
     */
    @Field(type = FieldType.Keyword)
    private String userName;

    /**
     * 客户端 IP 地址
     * <p>
     * <b>映射类型：</b> Ip
     * <br>
     * <b>高级特性：</b> 利用 ES 专用的 IP 类型，支持 CIDR 网段查询。
     * <br>例如：查询 {@code ip:"192.168.0.0/16"} 可找出该内网段的所有访问记录。
     */
    @Field(type = FieldType.Ip)
    private String ip;

    /**
     * 业务模块名称
     * <p>
     * <b>映射类型：</b> Keyword
     * <br>
     * <b>来源：</b> {@code @Auditable(module="订单中心")}
     */
    @Field(type = FieldType.Keyword)
    private String module;

    /**
     * 业务动作描述
     * <p>
     * 映射：Keyword (如 "Login", "SubmitOrder")
     */
    @Field(type = FieldType.Keyword)
    private String action;

    /**
     * HTTP 请求方式
     * <p>
     * 映射：Keyword (如 "POST", "GET")
     */
    @Field(type = FieldType.Keyword)
    private String requestMethod;

    /**
     * 请求 API 路径
     * <p>
     * <b>映射类型：</b> Keyword
     * <br>
     * <b>性能考量：</b> 这里仅作为 Keyword 存储，便于统计 "访问量最大的 Top 接口"。
     * 若需支持 URL 模糊匹配 (如 {@code *user*})，建议开启 Multi-Field 增加 Text 子字段。
     */
    @Field(type = FieldType.Keyword)
    private String requestUrl;

    /**
     * 请求入参快照 (JSON)
     * <p>
     * <b>映射类型：</b> Text (分词索引)
     * <br>
     * <b>分词器：</b> {@code standard}
     * <br>
     * <b>设计意图：</b> 参数内容不可预测。使用 Text 类型允许运维人员搜索参数中的关键信息（如订单号、商品ID）。
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String params;

    /**
     * 响应状态码
     * <p>
     * 映射：Integer (200, 500, 403)
     */
    @Field(type = FieldType.Integer)
    private Integer code;

    /**
     * 异常堆栈信息
     * <p>
     * <b>映射类型：</b> Text
     * <br>
     * <b>运维场景：</b> 全文检索报错详情。例如搜索 {@code "NullPointerException"} 快速定位所有发生空指针的业务场景。
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String errorMsg;

    /**
     * 执行耗时 (毫秒)
     * <p>
     * <b>映射类型：</b> Long
     * <br>
     * <b>性能分析：</b> 用于构建直方图 (Histogram) 或查询慢接口 (Range Query: {@code costTime > 1000})。
     */
    @Field(type = FieldType.Long)
    private Long costTime;
}