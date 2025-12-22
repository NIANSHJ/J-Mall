package com.nshj.mall.annotation;

import java.lang.annotation.*;

/**
 * 操作日志审计注解 (Operational Audit Annotation)
 * <p>
 * <b>架构定位：</b>
 * 位于基础设施层，作为 AOP (Aspect Oriented Programming) 切面的 <b>元数据锚点 (Metadata Anchor)</b>。
 * 它定义了审计日志系统的拦截边界，实现了业务逻辑与可观测性代码的完全解耦。
 * <p>
 * <b>运行时行为：</b>
 * 被标记的方法在执行时，将被 {@code LogAspect} 切面拦截，自动捕获以下上下文信息并异步通过消息队列投递至分析存储（如 ELK/ClickHouse）：
 * <ul>
 * <li><b>身份上下文：</b> 操作人 ID、用户名、所属租户。</li>
 * <li><b>环境上下文：</b> 客户端 IP、User-Agent、请求 URL。</li>
 * <li><b>执行上下文：</b> 方法入参、执行耗时、异常堆栈、操作结果状态。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 业务功能模块 (Business Module)
     * <p>
     * <b>语义定义：</b>
     * 标识当前操作所属的顶层业务领域或子系统。
     * <br>
     * <b>使用规范：</b>
     * 建议使用全局常量或枚举值，避免硬编码字符串，确保日志聚合分析时的维度统一性。
     * <br>
     * <b>示例：</b> "订单中心", "用户管理", "供应链子系统"
     *
     * @return 模块名称
     */
    String module() default "";

    /**
     * 业务操作行为 (Business Action)
     * <p>
     * <b>语义定义：</b>
     * 描述当前方法的具体业务意图或动作类型。
     * <br>
     * <b>审计价值：</b>
     * 用于生成人类可读的操作摘要，是安全审计和行为追踪的关键字段。
     * <br>
     * <b>示例：</b> "创建采购单", "导出财务报表", "重置用户密码"
     *
     * @return 行为描述
     */
    String action() default "";
}