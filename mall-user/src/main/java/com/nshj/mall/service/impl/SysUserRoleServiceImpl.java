package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nshj.mall.entity.SysUserRole;
import com.nshj.mall.mapper.SysUserRoleMapper;
import com.nshj.mall.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户-角色关联默认实现
 * <p>
 * <b>实现细节：</b>
 * 本类主要利用 {@link ServiceImpl} 提供的通用 CRUD 能力。
 * <br>在复杂的 RBAC 场景中，通常不需要在此类中编写额外逻辑，
 * 而是由上层 {@code SysUserService} 通过事务组合调用本服务的 {@code saveBatch} 或 {@code remove} 方法。
 *
 * @author nshj
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {
}