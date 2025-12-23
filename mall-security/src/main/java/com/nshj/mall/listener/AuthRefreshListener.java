package com.nshj.mall.listener;

import com.nshj.mall.constant.RocketMqConstants;
import com.nshj.mall.manager.DynamicAuthorizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 权限变更事件监听器
 * <p>
 * 监听系统权限刷新指令，采用 <b>广播模式 (Broadcasting)</b> 消费。
 * 确保集群中的每一个服务实例均能接收到通知，从而触发本地 JVM 缓存的重载与同步。
 *
 * @author nshj
 * @see DynamicAuthorizationManager#refreshRules(boolean)
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMqConstants.TOPIC_SYS_BROADCAST,
        consumerGroup = "auth-refresh-consumer-group",
        selectorExpression = RocketMqConstants.TAG_AUTH,
        messageModel = MessageModel.BROADCASTING
)
public class AuthRefreshListener implements RocketMQListener<String> {

    private final DynamicAuthorizationManager authorizationManager;

    /**
     * 接收广播消息并触发本地缓存刷新
     *
     * @param message 触发信号 (内容仅作为审计或时间戳参考，不直接参与业务逻辑)
     */
    @Override
    public void onMessage(String message) {
        log.info("接收到权限变更广播 [{}]，正在执行本地缓存重载...", message);

        // 委托鉴权管理器重新加载权限规则 (DB -> Redis -> Local Cache)
        authorizationManager.refreshRules(true);

        log.info("本地权限缓存重载完毕");
    }
}