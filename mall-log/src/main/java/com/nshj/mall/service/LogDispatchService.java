package com.nshj.mall.service;

import com.nshj.mall.entity.AuditLog;

/**
 * 日志分发服务接口 (Log Dispatch Service)
 * <p>
 * <b>架构定位：</b>
 * 定义日志处理的下游契约。
 * <p>
 * <b>设计意图：</b>
 * 遵循依赖倒置原则 (DIP)。上层切面 (Aspect) 仅依赖此接口，不感知具体实现。
 * <br>未来若架构演进（例如：从 RocketMQ 切换至 Kafka，或直接写入 ClickHouse），
 * 仅需替换实现类，无需修改切面逻辑。
 *
 * @author nshj
 * @since 1.0.0
 */
public interface LogDispatchService {

    /**
     * 分发日志
     *
     * @param auditLog 封装好的审计日志实体
     */
    void dispatch(AuditLog auditLog);
}