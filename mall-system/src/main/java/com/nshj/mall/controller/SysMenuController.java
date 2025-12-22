package com.nshj.mall.controller;

import com.nshj.mall.entity.SysMenu;
import com.nshj.mall.response.Result;
import com.nshj.mall.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 菜单资源适配器 (Menu Resource Adapter)
 * <p>
 * <b>架构定位：</b>
 * 系统资源 (Resource/Permission) 的管理入口。负责处理前端 UI 组件对菜单数据的查询与变更请求。
 * <p>
 * <b>核心职责：</b>
 * <ol>
 * <li><b>多视图适配：</b> 提供面向导航栏的 "递归树 (Tree)" 与面向表格管理的 "扁平列表 (Flat List)" 两种数据形态。</li>
 * <li><b>动态权限同步：</b> 所有的增删改操作均会触发 Spring Security 动态权限源的刷新或缓存清理，确保后端鉴权逻辑与数据库实时一致。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@RestController
@RequestMapping("/system/menu")
@Tag(name = "02.菜单管理", description = "系统功能菜单与权限标识配置")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /**
     * 获取全量菜单树 (Tree View)
     * <p>
     * <b>UI 场景：</b>
     * <ul>
     * <li>后台管理系统左侧的 <b>折叠导航栏 (Sidebar)</b>。</li>
     * <li>菜单管理页面新增/编辑时的 <b>上级菜单选择器 (TreeSelect)</b>。</li>
     * </ul>
     * <b>数据结构：</b>
     * 返回顶级根节点列表，子节点递归嵌套在 {@code children} 字段中。前端无需二次处理即可直接渲染 ElementUI/AntD 的 Tree 组件。
     *
     * @return 树形结构的菜单集合
     */
    @Operation(summary = "获取菜单树", description = "返回经过递归组装的树形结构数据 (用于导航栏渲染)")
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.success(sysMenuService.getTreeMenu());
    }

    /**
     * 检索菜单列表 (Flat List)
     * <p>
     * <b>UI 场景：</b>
     * 用于渲染 <b>菜单管理表格 (Table)</b>。
     * 相比于树形结构，扁平列表更适合展示表格详情、排序、以及进行关键字搜索过滤。
     *
     * @param menuName 菜单名称关键字 (可选，支持模糊匹配)
     * @return 按 {@code orderNum} 排序的扁平化菜单列表
     */
    @Operation(summary = "查询菜单列表", description = "根据名称模糊检索，返回扁平列表 (用于表格展示)")
    @GetMapping("/list")
    public Result<List<SysMenu>> list(@RequestParam(required = false) String menuName) {
        return Result.success(sysMenuService.getMenuList(menuName));
    }

    /**
     * 获取菜单详情
     * <p>
     * 用于 "编辑" 模态框的数据回显 (Form Rehydration)。
     *
     * @param id 菜单主键 ID
     * @return 菜单完整实体信息
     */
    @Operation(summary = "菜单详情", description = "根据ID获取单个菜单的属性信息")
    @GetMapping("/{id}")
    public Result<SysMenu> info(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }

    /**
     * 创建新菜单
     * <p>
     * <b>副作用 (Side Effect)：</b>
     * 新增菜单通常意味着新增了权限标识 (Permission Key)。
     * 业务层需确保同步更新 Redis 中的路由缓存，甚至触发网关层的路由重载。
     *
     * @param sysMenu 菜单实体 (需通过 @Validated 校验必填项)
     * @return 空响应
     */
    @Operation(summary = "新增菜单", description = "录入新的功能资源")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenu sysMenu) {
        sysMenuService.createMenu(sysMenu);
        return Result.success();
    }

    /**
     * 更新菜单属性
     * <p>
     * <b>场景：</b> 修改菜单名称、图标、前端路由地址或调整显示顺序。
     * <br><b>注意：</b> 修改后会自动触发权限刷新机制。
     *
     * @param sysMenu 菜单实体 (ID 必填)
     * @return 空响应
     */
    @Operation(summary = "修改菜单", description = "更新属性并自动触发权限刷新")
    @PutMapping
    public Result<Void> edit(@RequestBody SysMenu sysMenu) {
        sysMenuService.updateMenu(sysMenu);
        return Result.success();
    }

    /**
     * 批量删除菜单资源
     * <p>
     * <b>完整性约束 (Fail-Fast)：</b>
     * 为维护树结构拓扑的完整性，若目标菜单下挂载了 <b>子菜单 (Children)</b>，则禁止删除并抛出业务异常。
     * <br>用户必须先删除所有子节点，才能删除父节点。
     *
     * @param menuIds 待删除的菜单 ID 数组 (e.g., 1,2,3)
     * @return 空响应
     */
    @Operation(summary = "删除菜单", description = "物理删除菜单记录，需满足无子节点约束")
    @DeleteMapping("/{menuIds}")
    public Result<Void> remove(@PathVariable Long[] menuIds) {
        sysMenuService.removeMenuBatchByIds(Arrays.asList(menuIds));
        return Result.success();
    }
}