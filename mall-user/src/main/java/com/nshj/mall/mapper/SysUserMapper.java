package com.nshj.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nshj.mall.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息数据访问接口 (User Data Access Object)
 * <p>
 * <b>架构定位：</b>
 * 位于持久层 (Persistence Layer)，主要负责 {@link SysUser} 实体与数据库 {@code sys_user} 表之间的
 * 对象关系映射 (O/R Mapping) 及数据持久化交互。
 * <p>
 * <b>核心职能：</b>
 * <ul>
 * <li><b>基础 CRUD：</b> 继承 {@link BaseMapper}，自动装配标准化的增删改查方法 (如 {@code insert}, {@code updateById}, {@code selectById})。</li>
 * <li><b>领域支撑：</b> 作为用户领域模型 (User Domain) 的底层数据支撑组件，供上层业务逻辑调用。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}