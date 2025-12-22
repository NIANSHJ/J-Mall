package com.nshj.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nshj.mall.config.properties.JwtProperties;
import com.nshj.mall.constant.LogConstants;
import com.nshj.mall.constant.RedisConstants;
import com.nshj.mall.entity.SysUser;
import com.nshj.mall.exception.BusinessException;
import com.nshj.mall.model.dto.UserLoginDTO;
import com.nshj.mall.model.security.LoginUser;
import com.nshj.mall.service.AuthService;
import com.nshj.mall.service.SysMenuService;
import com.nshj.mall.service.SysRoleService;
import com.nshj.mall.service.SysUserService;
import com.nshj.mall.utils.JwtUtils;
import com.nshj.mall.utils.RedisCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务核心实现 (Core Authentication Service)
 * <p>
 * <b>技术架构特征：</b>
 * <ol>
 * <li><b>编排式认证 (Manual Orchestration):</b>
 * 不依赖 Spring Security 的 {@code AuthenticationManager} 黑盒，而是手动编排查库、比对密码、检查状态等流程，
 * 以便获得更细粒度的业务控制权（如精确的错误提示、登录日志埋点）。
 * </li>
 * <li><b>服务端状态管理 (Stateful JWT):</b>
 * 采用 "JWT + Redis" 混合模式。Redis 作为会话的"真理之源" (Source of Truth)，
 * 赋予了 JWT "可立即撤销" (Revocable) 的能力，解决了纯 JWT 无法登出或踢人的安全痛点。
 * </li>
 * <li><b>单一会话策略 (Single Session):</b>
 * Redis Key 采用 {@code user_token:{userId}} 结构。这意味着同一账号多次登录时，
 * 新生成的 Token 数据会直接覆盖旧数据，从而实现 "后登录踢掉前登录" 的互斥效果。
 * </li>
 * </ol>
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final RedisCache redisCache;
    private final HttpServletRequest request;

    @Override
    public Map<String, String> login(UserLoginDTO loginDTO) {
        log.info("认证请求 - 用户名: {}", loginDTO.getUsername());

        // --------------------------------------------------------------------
        // Step 1: 身份识别 (Identity Verification)
        // --------------------------------------------------------------------
        SysUser sysUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername()));

        if (sysUser == null) {
            // 安全策略：模糊化错误提示，防止恶意用户通过响应差异枚举系统内存在的账号。
            throw new BusinessException("用户名或密码错误");
        }

        // --------------------------------------------------------------------
        // Step 2: 凭证核验 (Credential Verification)
        // --------------------------------------------------------------------
        // 使用 BCrypt 算法比对明文密码与数据库中的哈希散列
        if (!passwordEncoder.matches(loginDTO.getPassword(), sysUser.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // --------------------------------------------------------------------
        // Step 3: 风控检查 (Risk Control)
        // --------------------------------------------------------------------
        if (sysUser.getStatus() == 0) {
            throw new BusinessException("账号已被冻结，请联系系统管理员");
        }

        // --------------------------------------------------------------------
        // Step 4: 上下文装载 (Context Assembly)
        // --------------------------------------------------------------------
        // 策略：登录时一次性加载所有权限并缓存。
        // 优势：后续鉴权过滤器 (Filter) 可直接从 Redis 读取，避免每次 HTTP 请求都查询数据库，极大提升吞吐量。
        List<String> permissions = sysMenuService.getPermsByUserId(sysUser.getId());
        List<String> roleKeys = sysRoleService.getRoleKeysByUserId(sysUser.getId());

        LoginUser loginUser = new LoginUser();
        BeanUtils.copyProperties(sysUser, loginUser);
        loginUser.setPermissions(permissions);
        loginUser.setRoleKeys(roleKeys);

        // --------------------------------------------------------------------
        // Step 5: 令牌签发 (Token Issuance)
        // --------------------------------------------------------------------
        // 生成 Token 唯一指纹 (JTI)，用于标识本次特定的会话
        String tokenUuid = UUID.randomUUID().toString();
        loginUser.setTokenUUID(tokenUuid);

        // 签发 JWT (Payload 包含: userId, username, tokenUuid)
        String token = jwtUtils.createToken(sysUser.getId(), sysUser.getUsername(), tokenUuid);

        // --------------------------------------------------------------------
        // Step 6: 会话持久化 (Session Persistence)
        // --------------------------------------------------------------------
        // Key 结构：user_token:{userId}
        // 逻辑：若 Key 已存在，Redis 会直接覆盖旧值。导致旧 Token (持有旧 UUID) 在后续过滤器校验时，
        // 因 UUID 不匹配而被拒绝访问，从而实现"踢人下线"。
        String redisKey = RedisConstants.USER_TOKEN_KEY + sysUser.getId();
        redisCache.setCacheObject(redisKey, loginUser, jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

        // --------------------------------------------------------------------
        // Step 7: 设置用户信息
        // --------------------------------------------------------------------
        request.setAttribute(LogConstants.LOG_ATTR_USER_ID, sysUser.getId());
        request.setAttribute(LogConstants.LOG_ATTR_USER_NAME, loginUser.getUsername());

        // --------------------------------------------------------------------
        // Step 8: 响应构建
        // --------------------------------------------------------------------
        Map<String, String> map = new HashMap<>();
        map.put("token", token);
        map.put("tokenPrefix", jwtProperties.getPrefix());
        return map;
    }

    @Override
    public void logout() {
        // 从 SecurityContext (线程局部变量) 获取当前认证主体
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            Long userId = loginUser.getId();

            // 核心动作：移除 Redis 缓存 -> 使 Token 即刻失效 (Revocation)
            // 客户端持有的 JWT 虽然签名仍然有效，但因对应的 Redis 数据已清除，过滤器将无法加载 UserDetails。
            redisCache.deleteObject(RedisConstants.USER_TOKEN_KEY + userId);
            log.info("用户注销成功 - UserId: {}", userId);
        }

        // 清理 ThreadLocal，防止线程池复用导致的数据污染
        SecurityContextHolder.clearContext();
    }
}