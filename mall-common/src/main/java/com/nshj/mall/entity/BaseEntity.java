package com.nshj.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局通用基础实体类
 * <p>
 * 定义数据表通用的标准字段，包括主键标识、数据审计（创建与更新时间）及逻辑删除标识。
 * 旨在通过继承方式统一数据模型结构，并结合 MyBatis-Plus 提供的自动填充与逻辑删除特性，
 * 简化业务实体类的定义并确保数据维护的一致性。
 *
 * @author nshj
 * @version 1.0.0
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 数据库主键标识
     * <p>
     * 采用 {@link IdType#AUTO} 策略，依赖数据库底层的自增机制生成唯一 ID。
     * 适用于单体架构或非分布式 ID 强需求的场景。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据记录创建时间
     * <p>
     * 标记数据的初始生成时间。
     * 配置了 {@link FieldFill#INSERT} 策略，在执行插入操作时，
     * 将由 {@link MetaObjectHandler} 自动拦截并填充当前系统时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 数据记录最后更新时间
     * <p>
     * 标记数据内容的最后一次变更时间。
     * 配置了 {@link FieldFill#INSERT_UPDATE} 策略，无论是插入还是更新操作，
     * 均会由自动填充处理器刷新该字段为当前系统时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除状态标识
     * <p>
     * 用于实现数据的软删除功能，保障数据的可追溯性。
     * <ul>
     * <li>0：表示数据有效（未删除）</li>
     * <li>1：表示数据已删除（逻辑删除）</li>
     * </ul>
     * 执行删除操作时，框架将自动更新此字段而非物理删除记录；执行查询时，会自动过滤已标记为删除的记录。
     */
    @TableLogic
    private Integer deleted;
}