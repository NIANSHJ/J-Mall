package com.nshj.mall.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户全量会话上下文视图 (Session Context VO)
 * <p>
 * <b>架构定位：</b>
 * 该类是前端单页应用 (SPA) <b>初始化引导阶段 (Bootstrap)</b> 的核心数据载体。
 * 通常对应 {@code /getRouters} 或 {@code /auth/info} 接口。
 * <p>
 * <b>设计模式 - 组合视图 (Composite View)：</b>
 * 通过继承 {@link UserVO} 复用基础档案数据 (Identity)，并扩展授权数据 (Authority)。
 * <br><b>收益：</b>
 * 实现了一次网络请求同时返回用户的 "身份信息 (Who am I)" 和 "权限信息 (What can I do)"，
 * 避免前端在页面加载时产生“瀑布式”的多次 API 调用，显著提升首屏加载速度。
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前登录用户全量上下文 (档案 + 权限)")
public class UserInfoVO extends UserVO {

    /**
     * 角色身份标识集 (Coarse-grained Roles)
     * <p>
     * <b>用途：</b> 粗粒度逻辑控制。
     * <br>
     * <b>前端场景：</b>
     * 1. <b>业务逻辑分支：</b> {@code if (roles.includes('admin')) { ... }}
     * 2. <b>区块级显隐：</b> 控制大的功能版块（如“系统管理”菜单组）的可见性。
     */
    @Schema(description = "角色标识集合", example = "[\"admin\", \"operation_manager\"]")
    private List<String> roleKeys;

    /**
     * 细粒度权限指令集 (Fine-grained Permissions)
     * <p>
     * <b>用途：</b> 原子级功能鉴权与动态路由驱动。
     * <br>
     * <b>前端场景：</b>
     * 1. <b>按钮级控制：</b> 配合自定义指令 (如 {@code v-permission="['user:add']"}) 控制按钮的渲染/移除。
     * 2. <b>动态路由生成：</b> 前端根据此列表与本地路由表比对，计算出当前用户可访问的最终路由树 (Route Tree)。
     */
    @Schema(description = "功能权限标识集合", example = "[\"system:user:query\", \"system:user:add\", \"system:role:edit\"]")
    private List<String> permissions;
}