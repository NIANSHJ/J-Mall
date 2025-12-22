package com.nshj.mall.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户详情/编辑表单视图对象 (Form View Object)
 * <p>
 * <b>架构定位：</b>
 * 专用于管理后台 "用户管理 - 编辑/详情" 场景的数据回显。
 * <p>
 * <b>设计模式 - 表单回填 (Form Rehydration)：</b>
 * 本类继承自 {@link UserVO} 以复用基础属性（如昵称、手机号），并扩展了关联关系的引用集合 (如 {@code roleIds})。
 * <br><b>目的：</b> 为前端表单组件提供完整的初始化状态，确保输入框 (Input) 和选择控件 (Select/Checkbox) 能正确“勾选”出当前数据库中已存在的记录。
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户详情上下文 (包含关联ID，用于表单回显)")
public class UserDetailVO extends UserVO {

    /**
     * 关联角色索引集合 (Associated Role References)
     * <p>
     * <b>UI 绑定目标：</b>
     * 通常绑定至前端的多选控件，如 Checkbox Group (复选框组) 或 Multi-Select (多选下拉框)。
     * <p>
     * <b>交互逻辑：</b>
     * 前端接收到此 ID 列表后，会将其与“所有可用角色列表”进行比对，
     * 将匹配的角色选项自动置为 <b>Selected (选中)</b> 状态。
     * <p>
     * <b>数据特性：</b>
     * 仅包含主键 ID，而非完整的角色对象。以最小的数据包大小满足 UI 状态复原需求。
     */
    @Schema(description = "已关联的角色ID集合 (用于复选框回显)", example = "[101, 102]")
    private List<Long> roleIds;
}