package com.nshj.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nshj.mall.entity.SysUser;
import com.nshj.mall.model.security.LoginUser;
import com.nshj.mall.model.vo.UserDetailVO;
import com.nshj.mall.model.vo.UserInfoVO;
import com.nshj.mall.response.Result;
import com.nshj.mall.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 用户管理接口适配器 (User Management Adapter)
 * <p>
 * <b>架构定位：</b>
 * 位于表现层 (Presentation Layer)，作为后台管理系统前端与用户领域服务之间的 RESTful 网关。
 * <p>
 * <b>核心职责：</b>
 * <ol>
 * <li><b>生命周期管理：</b> 提供企业员工账号的创建 (Onboarding)、维护、冻结与注销 (Offboarding) 入口。</li>
 * <li><b>上下文分发：</b> 负责将 HTTP 请求参数解析为业务实体，并将 Security 安全上下文注入业务层。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@RestController
@RequestMapping("/system/user")
@Tag(name = "01.用户管理", description = "后台员工账号全生命周期管理")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 检索用户列表 (Pagination)
     * <p>
     * <b>查询策略：</b> 支持多条件组合模糊检索 (Username/Phone)，结果默认按创建时间倒序排列。
     *
     * @param pageNum  页码 (默认 1)
     * @param pageSize 页容量 (默认 10)
     * @param username 账号关键字过滤 (可选)
     * @param phone    手机号关键字过滤 (可选)
     * @return 分页包装的用户实体列表
     */
    @Operation(summary = "用户列表", description = "分页检索系统用户，支持多维度模糊筛选")
    @GetMapping("/list")
    public Result<Page<SysUser>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      String username,
                                      String phone) {
        return Result.success(sysUserService.getUserPage(pageNum, pageSize, username, phone));
    }

    /**
     * 获取当前会话上下文 (Session Context)
     * <p>
     * <b>场景：</b> 前端单页应用 (SPA) 初始化引导 (Bootstrap)。
     * <br><b>逻辑：</b> 解析当前 JWT 令牌对应的身份主体，返回 "我是谁" (Profile) 及 "我能做什么" (Permissions)。
     *
     * @param loginUser <b>自动注入：</b> Spring Security 上下文中的 Principal 对象 (通过 {@code @AuthenticationPrincipal})。
     * @return 包含基础档案与权限集合的复合视图对象 {@link UserInfoVO}
     */
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的全量上下文 (档案 + 权限 + 角色)")
    @GetMapping("/info")
    public Result<UserInfoVO> info(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(sysUserService.getCurrentUserInfo(loginUser));
    }

    /**
     * 获取编辑视图详情 (Edit View Rehydration)
     * <p>
     * <b>场景：</b> 管理员点击 "编辑" 按钮时的数据回显。
     * <br><b>差异：</b> 相比普通详情，此接口会额外返回 "用户已拥有的角色 ID 列表"，用于前端复选框的回填。
     *
     * @param userId 目标用户 ID
     * @return 用户详情 VO (包含 roleIds)
     */
    @Operation(summary = "获取用户详情", description = "根据 ID 获取用户详情及关联角色，用于表单回填")
    @GetMapping("/{userId}")
    public Result<UserDetailVO> getInfo(@PathVariable Long userId) {
        return Result.success(sysUserService.getUserDetail(userId));
    }

    /**
     * 注册新员工 (Onboarding)
     * <p>
     * <b>副作用：</b>
     * 1. 数据库新增 {@code sys_user} 记录。
     * 2. 建立用户与初始角色的关联关系 (写入 {@code sys_user_role})。
     *
     * @param sysUser 用户表单数据
     * @return 空响应
     */
    @Operation(summary = "新增用户", description = "创建新账号并分配初始角色")
    @PostMapping
    public Result<Void> add(@RequestBody SysUser sysUser) {
        sysUserService.addUser(sysUser);
        return Result.success();
    }

    /**
     * 更新员工档案 (Profile Update)
     * <p>
     * <b>限制：</b> 仅更新基础资料与角色分配，<b>不包含</b>密码修改逻辑。
     * <br><b>注意：</b> 修改成功后，后端会触发缓存清理机制。
     *
     * @param sysUser 用户表单数据 (必须包含 ID)
     * @return 空响应
     */
    @Operation(summary = "修改用户", description = "更新基础信息或角色。操作成功后将强制刷新该用户的权限缓存")
    @PutMapping
    public Result<Void> edit(@RequestBody SysUser sysUser) {
        sysUserService.updateUser(sysUser);
        return Result.success();
    }

    /**
     * 批量注销员工 (Offboarding)
     * <p>
     * <b>安全策略：</b> 自动识别并拒绝删除 "超级管理员" (admin) 账号，防止系统死锁。
     *
     * @param userIds 用户 ID 数组 (e.g., 1,2,3)
     * @return 空响应
     */
    @Operation(summary = "删除用户", description = "批量逻辑删除用户，并级联清理关联数据与 Token 缓存")
    @DeleteMapping("/{userIds}")
    public Result<Void> remove(@PathVariable Long[] userIds) {
        sysUserService.removeUserBatchByIds(Arrays.asList(userIds));
        return Result.success();
    }

    /**
     * 强制重置凭证 (Admin Override)
     * <p>
     * <b>场景：</b> 员工忘记密码或账号被盗时的紧急干预。
     * <br><b>行为：</b> 管理员直接指定新密码，不校验旧密码。
     *
     * @param sysUser 仅包含 ID 和 NewPassword 的载体对象
     * @return 空响应
     */
    @Operation(summary = "重置密码", description = "管理员强制重置指定用户的登录密码")
    @PutMapping("/resetPwd")
    public Result<Void> resetPwd(@RequestBody SysUser sysUser) {
        sysUserService.resetPwd(sysUser);
        return Result.success();
    }
}