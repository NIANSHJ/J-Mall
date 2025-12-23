package com.nshj.mall.service.impl;

import com.nshj.mall.constant.RocketMqConstants;
import com.nshj.mall.entity.AuditLog;
import com.nshj.mall.service.LogDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 日志分发服务 RocketMQ 实现类
 * <p>
 * <b>架构定位：</b>
 * 作为日志数据的 "生产者 (Producer)"，负责将 {@link AuditLog} 实体投递至消息队列。
 * <p>
 * <b>核心特性：</b>
 * 1. <b>异步非阻塞：</b> 使用 {@code asyncSend} 方法，确保日志记录操作不会阻塞主业务线程 (如订单创建、支付等)。
 * 2. <b>链路追踪：</b> 将 {@code TraceId} 注入 Message Key，便于在 MQ 控制台追踪消息轨迹。
 * 3. <b>容错处理：</b> 仅在回调中记录 Error Log，即便是日志投递失败也不影响核心业务的事务提交（Best Effort 模式）。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogDispatchServiceImpl implements LogDispatchService {

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void dispatch(AuditLog auditLog) {
        // 构建消息体，注入 TraceId 作为 Key 以便于检索
        Message<AuditLog> message = MessageBuilder.withPayload(auditLog)
                .setHeader(RocketMQHeaders.KEYS, auditLog.getTraceId())
                .build();

        // 执行异步发送
        rocketMQTemplate.asyncSend(RocketMqConstants.TOPIC_MALL_LOG + RocketMqConstants.TOPIC_TAG_SEPARATOR + RocketMqConstants.TAG_LOG_AUDIT,
                message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        // 正常场景下静默处理，避免日志爆炸
                    }

                    @Override
                    public void onException(Throwable e) {
                        // 异常降级：记录错误日志，后续可通过 Log 文件通过 Filebeat 等工具补救
                        log.error("审计日志投递 MQ 失败, TraceId: {}", auditLog.getTraceId(), e);
                    }
                });
    }
}