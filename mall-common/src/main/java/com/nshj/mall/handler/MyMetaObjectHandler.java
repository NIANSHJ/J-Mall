package com.nshj.mall.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 实现 MyBatis-Plus 提供的 {@link MetaObjectHandler} 接口，用于拦截数据库操作并自动注入审计字段。
 * 通过统一管控 `createTime` 和 `updateTime` 的赋值逻辑，消除了业务层手动维护时间戳的冗余代码，
 * 确保了数据生命周期记录的统一性与准确性。
 *
 * @author nshj
 * @version 1.0.0
 * @see com.baomidou.mybatisplus.annotation.FieldFill
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作元数据填充策略
     * <p>
     * <b>触发机制：</b>
     * 当执行 SQL 插入操作（INSERT）时，框架回调此方法。
     * <p>
     * <b>执行逻辑：</b>
     * <ul>
     * <li>捕获当前系统时间，确保创建时间与初始更新时间在毫秒级严格一致。</li>
     * <li>调用 {@code strictInsertFill} 方法：仅当实体属性为 {@code null} 时才执行填充。
     * 若业务层已手动对字段赋值，则以业务层逻辑为准，避免覆盖。</li>
     * </ul>
     *
     * @param metaObject 元对象句柄，包含原始实体对象及操作上下文
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 获取当前系统时间，作为统一的时间戳锚点
        LocalDateTime now = LocalDateTime.now();

        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        // 填充更新时间 (新记录的最后更新时间即为创建时间)
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * 更新操作元数据填充策略
     * <p>
     * <b>触发机制：</b>
     * 当执行 SQL 更新操作（UPDATE）时，框架回调此方法。
     * <p>
     * <b>执行逻辑：</b>
     * 强制刷新 {@code updateTime} 字段为当前系统时间，以记录数据最后一次变更的时间戳。
     *
     * @param metaObject 元对象句柄
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}