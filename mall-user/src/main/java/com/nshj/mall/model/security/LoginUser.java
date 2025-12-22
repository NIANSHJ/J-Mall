package com.nshj.mall.model.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全认证主体适配器 (Security Principal Adapter)
 * <p>
 * <b>架构定位：</b>
 * 该类是系统业务身份 ({@code SysUser}) 与 Spring Security 框架身份 ({@link UserDetails}) 之间的核心适配桥梁。
 * <p>
 * <b>设计策略 - 数据扁平化 (Data Flattening)：</b>
 * 本类不直接聚合 {@code SysUser} 实体对象，而是提取其核心字段独立存储。
 * <ul>
 * <li><b>解耦性：</b> 阻断 Security 模块与 System 业务模块实体的强耦合。</li>
 * <li><b>安全性：</b> 避免将数据库实体中不必要的敏感字段（如加盐随机数、修改时间）暴露到安全上下文中。</li>
 * <li><b>序列化优化：</b> 专门针对 Redis 存储进行瘦身。相比存储复杂的实体对象，扁平化结构在 JSON 序列化/反序列化时性能更高，且不易出现版本兼容问题。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {

    /**
     * 主键 ID
     * <p>
     * 业务系统的唯一标识，通常用于日志记录 (MDC) 或业务关联。
     */
    private Long id;

    /**
     * 主体标识符 (Principal Name)
     * <p>
     * Spring Security 认证上下文中的核心检索 Key，对应 {@code sys_user.username}。
     */
    private String username;

    /**
     * 认证凭证 (Hashed Credential)
     * <p>
     * <b>安全策略：</b>
     * 仅在 {@code DaoAuthenticationProvider} 验证过程中使用。
     * <br>必须标记 {@code @JsonIgnore}，确保该字段<b>永远不会</b>被序列化写入 Redis 缓存或返回给前端，防止哈希泄露。
     */
    @JsonIgnore
    private String password;

    /**
     * 显示名称
     * <p>
     * 用于 UI 展示的非敏感信息 (如 "欢迎您，张三")。
     */
    private String nickname;

    /**
     * 手机号码
     * <p>
     * 敏感隐私数据，默认配置为不序列化输出，保护用户隐私。
     */
    @JsonIgnore
    private String phone;

    /**
     * 头像资源 URI
     */
    private String avatar;

    /**
     * 账户原始状态码
     * <p>
     * 存储数据库原始值 (1: 正常, 0: 停用)，用于 {@link #isEnabled()} 的逻辑判断。
     */
    private Integer status;

    /**
     * 角色标识集合 (Role Keys)
     * <p>
     * <b>用途：</b> 粗粒度权限控制。
     * <br><b>场景：</b> 主要供前端使用 (如 {@code v-if="hasRole('admin')"}) 控制 UI 菜单或区块的显隐。
     */
    private List<String> roleKeys;

    /**
     * 权限标识集合 (Permission Strings)
     * <p>
     * <b>用途：</b> 细粒度权限控制的数据源 (Source of Truth)。
     * <br><b>持久化策略：</b>
     * 此字段会被序列化到 Redis。相较于存储复杂的 {@code GrantedAuthority} 对象，
     * 仅存储字符串列表 (List&lt;String&gt;) 可大幅降低 Redis 内存占用并提升序列化速度。
     */
    private List<String> permissions;

    /**
     * 令牌唯一指纹 (Token JTI/UUID)
     * <p>
     * <b>安全机制：</b> 单点登录互斥 (Mutual Exclusion) / 踢人下线。
     * <br><b>原理：</b>
     * 每次登录生成新的 UUID。过滤器校验时，比对请求 Token 中的 UUID 与 Redis 中存储的 {@code login_user_key} 中的 UUID。
     * 若不一致，说明该账号已在其他设备登录（导致 Redis 值被覆盖），当前请求将被视为 "无效令牌" 并拒绝。
     */
    private String tokenUUID;

    /**
     * 框架鉴权对象集合 (Security Authorities)
     * <p>
     * <b>瞬态属性 (Transient)：</b>
     * 标记为 {@code @JsonIgnore}，<b>不参与 Redis 序列化</b>。
     * <br><b>原因：</b>
     * 1. {@code SimpleGrantedAuthority} 在 Jackson 反序列化时容易出现构造器匹配问题。
     * 2. 避免数据冗余 (数据源已存在于 {@code permissions} 字段，反序列化后可重新生成)。
     */
    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;

    /**
     * 获取鉴权对象集合 (Lazy Loading)
     * <p>
     * <b>实现逻辑：</b>
     * 懒加载模式。当且仅当 Security 框架（如鉴权过滤器）调用此方法时，
     * 才将 {@code permissions} 字符串列表动态转换为框架所需的 {@code GrantedAuthority} 对象列表。
     *
     * @return 权限对象集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null && permissions != null) {
            authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账户是否未过期
     * <p>
     * 业务系统暂未启用 "账号有效期" 功能，默认返回 true。
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定
     * <p>
     * 业务系统暂未启用 "密码输错锁定" 功能（或由外部逻辑控制），默认返回 true。
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证是否未过期
     * <p>
     * 业务系统暂未启用 "强制定期修改密码" 功能，默认返回 true。
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否可用 (Enabled)
     * <p>
     * <b>业务映射：</b>
     * 将数据库状态码 (1) 映射为布尔值。
     * <br>若返回 {@code false}，Spring Security 将直接拒绝登录并抛出 {@code DisabledException}。
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}