package com.nshj.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nshj.mall.entity.SysUser;
import com.nshj.mall.model.security.LoginUser;
import com.nshj.mall.model.vo.UserDetailVO;
import com.nshj.mall.model.vo.UserInfoVO;

import java.util.List;

/**
 * 用户领域服务契约 (User Domain Service)
 * <p>
 * <b>职责边界：</b>
 * <ol>
 * <li><b>核心业务：</b> 处理员工档案管理、入职(新增)、离职(删除)、调岗(角色变更)等核心流程。</li>
 * <li><b>安全聚合：</b> 负责密码加密 (BCrypt)、敏感数据清洗以及 Token 缓存生命周期管理。</li>
 * <li><b>关系维护：</b> 维护 {@code SysUser} (聚合根) 与 {@code SysRole} (关联对象) 之间的聚合关系。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 检索分页数据
     *
     * @param pageNum  当前页码
     * @param pageSize 页容量
     * @param username 账号过滤器 (模糊匹配)
     * @param phone    电话过滤器 (模糊匹配)
     * @return MyBatis Plus 分页对象
     */
    Page<SysUser> getUserPage(Integer pageNum, Integer pageSize, String username, String phone);

    /**
     * 组装当前登录者上下文
     * <p>
     * <b>性能策略：</b>
     * 采用 <b>Hybrid (混合)</b> 获取模式：
     * <ul>
     * <li><b>基础资料 (Avatar, Nickname)：</b> 强制回表查询 (DB)，确保用户修改资料后刷新页面即刻可见。</li>
     * <li><b>权限数据 (Permissions)：</b> 优先读取缓存或 SecurityContext，以保证高频鉴权接口的性能。</li>
     * </ul>
     *
     * @param loginUser 安全框架注入的 Principal
     * @return 聚合视图对象 {@link UserInfoVO}
     */
    UserInfoVO getCurrentUserInfo(LoginUser loginUser);

    /**
     * 组装编辑回显数据
     *
     * @param userId 目标 ID
     * @return 详情视图 (UserDetailVO = UserVO + RoleIds)
     */
    UserDetailVO getUserDetail(Long userId);

    /**
     * 执行新增用户业务
     * <p>
     * <b>原子操作 (Transactional)：</b>
     * 1. 校验账号唯一性 (Duplicate Check)。
     * 2. 密码加盐哈希处理 (BCrypt Encoding)。
     * 3. 插入用户主表。
     * 4. 插入用户-角色关联表。
     *
     * @param sysUser 领域实体
     */
    void addUser(SysUser sysUser);

    /**
     * 执行更新用户业务
     * <p>
     * <b>副作用 (Side Effect)：</b>
     * 修改生效后，必须强制清理该用户的 Redis Token 缓存。
     * <br>这会导致该用户在下一次请求时触发 "权限重载" 或 "被迫下线" (取决于 Token 校验逻辑)，确保权限变更即时生效。
     *
     * @param sysUser 领域实体
     */
    void updateUser(SysUser sysUser);

    /**
     * 执行批量删除业务
     * <p>
     * <b>数据完整性：</b>
     * 级联删除 {@code sys_user_role} 中间表数据，防止产生"孤儿数据" (Orphan Data)。
     *
     * @param userIds ID 集合
     */
    void removeUserBatchByIds(List<Long> userIds);

    /**
     * 执行密码重置业务
     * <p>
     * 管理员强制重置，无需校验旧密码。
     *
     * @param sysUser 包含 ID 和新明文密码的实体
     */
    void resetPwd(SysUser sysUser);

    /**
     * 查询角色关联索引
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表 (List of Role Primary Keys)
     */
    List<Long> getRoleIdsByUserId(Long userId);
}