package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nshj.mall.entity.SysRoleMenu;
import com.nshj.mall.mapper.SysRoleMenuMapper;
import com.nshj.mall.service.SysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 角色-菜单关联默认实现
 * <p>
 * <b>实现细节：</b>
 * 本类主要利用 {@link ServiceImpl} 提供的通用 CRUD 能力。
 * <br>在角色授权（Granting）操作中，通常采用 "先删后插" (Delete-Then-Insert) 的策略，
 * 本服务负责执行这两种底层的原子数据操作。
 *
 * @author nshj
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {
}