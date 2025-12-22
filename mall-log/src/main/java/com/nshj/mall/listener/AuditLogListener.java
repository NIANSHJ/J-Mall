package com.nshj.mall.listener;

import com.nshj.mall.constant.LogConstants;
import com.nshj.mall.constant.RocketMqConstants;
import com.nshj.mall.entity.AuditLog;
import com.nshj.mall.repository.SysLogEsRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志消息消费者 (Audit Log Message Listener)
 * <p>
 * <b>架构定位：</b>
 * 位于数据消费层，负责从 RocketMQ 订阅审计日志，并将其持久化至 Elasticsearch。
 * <p>
 * <b>核心特性：</b>
 * 1. <b>双重触发机制：</b> 支持 "按量 (Size-based)" 和 "按时 (Time-based)" 两种维度的批量提交，平衡了吞吐量与实时性。
 * 2. <b>线程安全设计：</b> 使用 {@code synchronized} 保护内存缓冲区，解决多线程并发写入与定时任务读取之间的竞争问题。
 * 3. <b>优雅停机：</b> 利用 {@code @PreDestroy} 钩子，在服务关闭前强制排空缓冲区，防止数据丢失。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMqConstants.TOPIC_MALL_LOG,
        consumerGroup = "audit-log-consumer-group",
        selectorExpression = RocketMqConstants.TAG_LOG_AUDIT
)
public class AuditLogListener implements RocketMQListener<AuditLog> {

    private final SysLogEsRepository sysLogEsRepository;

    /**
     * 内存缓冲区 (In-Memory Buffer)
     * <p>
     * 用于暂存从 MQ 消费到的日志消息，等待批量写入。
     */
    private List<AuditLog> buffer = new ArrayList<>(LogConstants.BATCH_SIZE);

    /**
     * 消息监听回调 (Message Consumption)
     * <p>
     * <b>执行逻辑：</b>
     * 将单条消息加入缓冲区。若缓冲区已满 ({@code >= 1000})，触发同步刷盘。
     *
     * @param auditLog 反序列化后的日志实体
     */
    @Override
    public void onMessage(AuditLog auditLog) {
        synchronized (this) {
            buffer.add(auditLog);
            if (buffer.size() >= LogConstants.BATCH_SIZE) {
                flush();
            }
        }
    }

    /**
     * 定时强制刷盘 (Scheduled Flush)
     * <p>
     * <b>调度策略：</b>
     * 每隔 10 秒执行一次。
     * <br>
     * <b>目的：</b>
     * 兜底策略。确保在低流量时段，滞留在缓冲区中的少量日志也能及时入库。
     */
    @Scheduled(fixedRate = LogConstants.FLUSH_INTERVAL * 1000)
    public void scheduledFlush() {
        flush();
    }

    /**
     * 核心刷盘逻辑 (Core Flush Logic)
     * <p>
     * <b>实现细节：</b>
     * 1. <b>临界区保护：</b> 快速交换 {@code buffer} 引用，将原缓冲区提出来处理，同时创建一个新缓冲区供后续使用。锁持有时间极短。
     * 2. <b>批量写入：</b> 调用 ES Repository 的 {@code saveAll} 接口执行 Bulk Request。
     * 3. <b>异常处理：</b> 捕获入库异常，防止消费线程崩溃 (生产环境应配合死信队列或重试机制)。
     */
    private void flush() {
        List<AuditLog> tempBuffer;
        // 临界区：只做引用交换，不做耗时 IO
        synchronized (this) {
            if (buffer.isEmpty()) {
                return;
            }
            tempBuffer = buffer;
            buffer = new ArrayList<>(LogConstants.BATCH_SIZE);
        }

        try {
            long startTime = System.currentTimeMillis();

            // 执行 IO 操作 (释放锁后执行，提高并发度)
            sysLogEsRepository.saveAll(tempBuffer);

            log.info("审计日志批量入库ES成功，条数: {}, 耗时: {}ms",
                    tempBuffer.size(), (System.currentTimeMillis() - startTime));

        } catch (Exception e) {
            // 生产环境建议：这里可以将失败的 tempBuffer 丢入 "死信队列" 或 "本地文件" 做兜底
            log.error("批量写入 ES 失败", e);
        }
    }

    /**
     * 生命周期销毁钩子 (Lifecycle Hook)
     * <p>
     * <b>触发时机：</b> Spring 容器关闭前。
     * <br>
     * <b>作用：</b> 强制执行一次刷盘，确保内存中剩余的日志数据安全落盘。
     */
    @PreDestroy
    public void destroy() {
        log.info("服务关闭，清理剩余日志...");
        flush();
    }
}