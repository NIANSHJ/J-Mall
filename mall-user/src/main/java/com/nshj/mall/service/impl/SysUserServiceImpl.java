package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nshj.mall.constant.RedisConstants;
import com.nshj.mall.entity.SysUser;
import com.nshj.mall.entity.SysUserRole;
import com.nshj.mall.exception.BusinessException;
import com.nshj.mall.mapper.SysUserMapper;
import com.nshj.mall.model.security.LoginUser;
import com.nshj.mall.model.vo.UserDetailVO;
import com.nshj.mall.model.vo.UserInfoVO;
import com.nshj.mall.service.SysUserRoleService;
import com.nshj.mall.service.SysUserService;
import com.nshj.mall.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务核心实现 (Core User Implementation)
 * <p>
 * <b>技术架构关注点：</b>
 * <ol>
 * <li><b>事务原子性 (Atomicity):</b> 使用 {@code @Transactional} 确保用户主表与角色关联表 ({@code sys_user_role}) 的操作要么全成，要么全败。</li>
 * <li><b>缓存一致性 (Cache Consistency):</b> 采用 "Cache Eviction" (缓存驱逐) 策略。当用户资料或权限变更落库后，主动删除 Redis 中的 Token 缓存，迫使下一次请求重新加载最新数据。</li>
 * <li><b>防御性编程 (Defensive Programming):</b> 在 Update 接口中显式将 {@code username} 和 {@code password} 置空，防止因前端恶意传参导致核心凭证被篡改。</li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;
    private final RedisCache redisCache;

    @Override
    public Page<SysUser> getUserPage(Integer pageNum, Integer pageSize, String username, String phone) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 动态拼接查询条件 (Dynamic Query Construction)
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .like(StringUtils.hasText(phone), SysUser::getPhone, phone)
                .orderByDesc(SysUser::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    public UserInfoVO getCurrentUserInfo(LoginUser loginUser) {
        // 1. 强制回表查询 Profile
        // 目的：虽然 LoginUser 中有基础信息，但可能是旧的。为了确保用户修改昵称/头像后刷新页面立即生效，此处查 DB。
        SysUser sysUser = this.getById(loginUser.getId());
        if (sysUser == null) {
            throw new BusinessException("Session异常：关联的用户主体已不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(sysUser, vo);

        // 2. 从 SecurityContext/Redis 中获取权限
        // 目的：权限数据量大且变更频率低，直接复用 Token 解析出的缓存数据，避免重复查询多张 RBAC 表。
        vo.setRoleKeys(loginUser.getRoleKeys());
        vo.setPermissions(loginUser.getPermissions());

        return vo;
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        SysUser sysUser = this.getById(userId);
        if (sysUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        UserDetailVO detailVO = new UserDetailVO();
        BeanUtils.copyProperties(sysUser, detailVO);

        // 填充关联数据，用于前端复选框 (Checkbox Group) 回显
        detailVO.setRoleIds(getRoleIdsByUserId(userId));

        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(SysUser sysUser) {
        // 1. 唯一性守卫 (Uniqueness Guard)
        if (!checkUsernameUnique(sysUser.getUsername())) {
            throw new BusinessException("新增失败：账号 '" + sysUser.getUsername() + "' 已被占用");
        }

        // 2. 凭证加固 (Credential Hardening)
        // 若前端未传密码，赋予默认初始密码，并进行 BCrypt 加盐哈希
        String rawPassword = StringUtils.hasText(sysUser.getPassword()) ? sysUser.getPassword() : "123456";
        sysUser.setPassword(passwordEncoder.encode(rawPassword));
        sysUser.setStatus(1); // 默认为激活状态

        // 3. 落库
        this.save(sysUser);
        insertUserRole(sysUser.getId(), sysUser.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser sysUser) {
        Long userId = sysUser.getId();

        // 1. 安全过滤：禁止通过此普通更新接口修改核心凭证
        // 防止"质量分配"攻击 (Mass Assignment Vulnerability)
        sysUser.setUsername(null);
        sysUser.setPassword(null);

        // 2. 更新主表
        this.updateById(sysUser);

        // 3. 更新关联表 (采用 "全删全插" 策略)
        // 相比于计算增量 (Diff)，直接删除旧关系再插入新关系在编码上更简洁健壮。
        if (sysUser.getRoleIds() != null) {
            sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId));
            insertUserRole(userId, sysUser.getRoleIds());
        }

        // 4. 缓存驱逐 (Cache Eviction)
        // 关键步骤：资料或角色变更后，必须清除 Redis 中的 Token 缓存。
        // 这将导致该用户发起的下一次请求无法在 Filter 中找到缓存，从而触发重新登录或逻辑上的"权限刷新"。
        clearUserCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserBatchByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) return;

        // 1. 核心守卫：保护超级管理员 (ID=1)
        if (userIds.contains(1L)) {
            throw new BusinessException("操作拒绝：系统超级管理员不可删除");
        }

        // 2. 级联清除关联数据 (物理删除)
        sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));

        // 3. 清除用户主体 (逻辑删除，由 MyBatis Plus 的 @TableLogic 自动处理)
        this.removeBatchByIds(userIds);

        // 4. 批量缓存驱逐
        List<String> keys = userIds.stream()
                .map(id -> RedisConstants.USER_TOKEN_KEY + id)
                .collect(Collectors.toList());
        redisCache.deleteObject(keys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(SysUser sysUser) {
        // 构建纯净的 Update Wrapper
        // 仅修改密码字段，防止前端恶意混入其他字段 (如 status=1) 导致越权修改
        SysUser cleanUser = new SysUser();
        cleanUser.setId(sysUser.getId());
        cleanUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));

        boolean updated = this.updateById(cleanUser);
        if (!updated) {
            throw new BusinessException("操作失败：目标用户不存在");
        }

        // 密码变更后，必须清除缓存迫使旧 Token 失效，强制用户重新登录
        clearUserCache(sysUser.getId());
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return sysUserRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getRoleId)
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 私有辅助方法 (Private Helper Methods)
    // ==========================================

    /**
     * 检查用户名是否唯一
     */
    private boolean checkUsernameUnique(String username) {
        return !this.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    /**
     * 批量插入用户-角色关联
     */
    private void insertUserRole(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) return;

        List<SysUserRole> list = roleIds.stream()
                .map(roleId -> new SysUserRole(userId, roleId))
                .collect(Collectors.toList());
        sysUserRoleService.saveBatch(list);
    }

    /**
     * 移除 Redis 中的用户 Token 缓存
     */
    private void clearUserCache(Long userId) {
        redisCache.deleteObject(RedisConstants.USER_TOKEN_KEY + userId);
    }
}