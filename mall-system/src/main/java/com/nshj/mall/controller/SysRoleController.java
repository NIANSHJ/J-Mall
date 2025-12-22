package com.nshj.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nshj.mall.entity.SysRole;
import com.nshj.mall.response.Result;
import com.nshj.mall.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 角色管理接口适配器 (Role Management Adapter)
 * <p>
 * <b>架构定位：</b>
 * RBAC (Role-Based Access Control) 模型的核心枢纽。
 * <br>负责连接 "用户(User)" 与 "资源(Menu)"，定义系统的身份与权利。
 * <p>
 * <b>安全规约：</b>
 * 角色操作涉及系统核心安全策略，所有写操作（增删改）建议在网关层配合审计日志 (Audit Log) 进行记录，以便追踪权限变更历史。
 *
 * @author nshj
 * @since 1.0.0
 */
@RestController
@RequestMapping("/system/role")
@Tag(name = "03.角色管理", description = "RBAC模型核心：角色定义与权限分配")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /**
     * 检索角色列表 (分页)
     * <p>
     * <b>查询策略：</b>
     * 默认按 {@code sort_order} (排序权重) 升序排列，以便前端展示时高频使用的角色靠前显示。
     *
     * @param pageNum  当前页码 (默认1)
     * @param pageSize 每页条数 (默认10)
     * @param name     角色名称关键字 (可选，支持模糊查询)
     * @return 分页包装的角色实体列表
     */
    @Operation(summary = "角色列表", description = "分页检索角色，支持按名称筛选")
    @GetMapping("/list")
    public Result<Page<SysRole>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String name) {
        return Result.success(sysRoleService.getRolePage(pageNum, pageSize, name));
    }

    /**
     * 获取角色详情
     *
     * @param id 角色主键
     * @return 角色基础信息
     */
    @Operation(summary = "角色详情", description = "获取角色的基础属性")
    @GetMapping("/{id}")
    public Result<SysRole> info(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    /**
     * 创建新角色
     * <p>
     * <b>关键约束：</b>
     * 必须确保角色标识 ({@code roleKey}) 的全局唯一性（如 `admin`, `hr_manager`），
     * 因为后端代码通常依赖此标识进行逻辑判断（例如 {@code @PreAuthorize("hasRole('admin')")}）。
     *
     * @param sysRole 角色实体 (需 @Validated 校验必填项)
     * @return 空响应
     */
    @Operation(summary = "新增角色", description = "创建新的身份定义")
    @PostMapping
    public Result<Void> add(@RequestBody SysRole sysRole) {
        sysRoleService.addRole(sysRole);
        return Result.success();
    }

    /**
     * 更新角色属性
     * <p>
     * <b>缓存一致性风险：</b>
     * 若修改了 "角色状态" (禁用) 或 "权限字符"，Service 层必须清理 Redis 中与该角色关联的所有用户 Token 缓存，
     * 迫使相关用户在下一次请求时重新认证，从而立即应用最新的权限策略。
     *
     * @param sysRole 角色实体 (ID 必填)
     * @return 空响应
     */
    @Operation(summary = "修改角色", description = "更新角色名称、权限字符或状态")
    @PutMapping
    public Result<Void> edit(@RequestBody SysRole sysRole) {
        sysRoleService.updateRole(sysRole);
        return Result.success();
    }

    /**
     * 批量删除角色
     * <p>
     * <b>防御性编程 (Defensive Programming)：</b>
     * 执行删除前，系统将检查该角色下是否仍挂载有 "用户"。
     * <br>若存在关联用户，将拒绝删除并抛出业务异常 (Referential Integrity Error)，防止用户变成 "无身份游魂"。
     *
     * @param roleIds 角色 ID 数组
     * @return 空响应
     */
    @Operation(summary = "删除角色", description = "批量逻辑删除，需满足无关联用户约束")
    @DeleteMapping("/{roleIds}")
    public Result<Void> remove(@PathVariable("roleIds") Long[] roleIds) {
        sysRoleService.removeRoleBatchByIds(Arrays.asList(roleIds));
        return Result.success();
    }

    // ==========================================
    // RBAC 授权接口 (Authorization / Granting)
    // ==========================================

    /**
     * 获取权限回显数据 (Form Rehydration)
     * <p>
     * <b>UI 交互原理：</b>
     * 前端权限树 (Tree Component) 渲染通常需要两个数据源：
     * <ol>
     * <li><b>全量树：</b> 调用 {@code /system/menu/tree} 加载完整的菜单结构。</li>
     * <li><b>已选集合：</b> 调用本接口，返回当前角色 "已勾选" 的叶子节点 ID 集合。</li>
     * </ol>
     * 前端将两者合并，自动计算出复选框 (Checkbox) 的 "选中" 和 "半选" 状态。
     *
     * @param id 角色 ID
     * @return 仅包含菜单 ID 的列表 (List of Menu IDs)
     */
    @Operation(summary = "获取角色拥有的菜单ID", description = "用于前端权限树的勾选状态回显")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable Long id) {
        return Result.success(sysRoleService.getMenuIdsByRoleId(id));
    }
}