package com.nshj.mall.utils;

import com.nshj.mall.model.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类 (Security Context Utilities)
 * <p>
 * <b>架构定位：</b>
 * 位于基础设施层，作为 Spring Security 框架与业务逻辑之间的 <b>适配桥梁 (Adapter Bridge)</b>。
 * 它屏蔽了底层 {@link SecurityContextHolder} 对 {@code ThreadLocal} 的操作细节，提供了一组静态方法来方便地获取当前登录用户的上下文信息。
 * <p>
 * <b>设计特性：</b>
 * 1. <b>空安全 (Null-Safety)：</b> 所有方法均内置了防御式编程逻辑，防止因未登录或上下文丢失导致的 NPE (NullPointerExceptoin)。
 * 2. <b>强类型封装：</b> 将通用的 {@code Object} 主体转换为业务专属的 {@link LoginUser} 对象。
 *
 * @author nshj
 * @since 1.0.0
 */
@Component
public class SecurityUtils {

    /**
     * 获取当前登录用户主体 (Login User Principal)
     * <p>
     * <b>实现逻辑：</b>
     * 从当前线程的 {@code SecurityContext} 中提取 {@code Authentication} 对象，
     * 并将其 {@code principal} 属性强制转换为系统定义的 {@link LoginUser}。
     * <p>
     * <b>容错处理：</b>
     * 若当前未登录、上下文为空或 Principal 类型不匹配，均安全返回 {@code null}。
     *
     * @return 扩展后的用户信息对象，若无上下文则返回 null
     */
    public LoginUser getLoginUser() {
        try {
            Authentication authentication = getAuthentication();
            if (authentication == null) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof LoginUser) {
                return (LoginUser) principal;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取原生鉴权对象 (Authentication Object)
     * <p>
     * <b>底层交互：</b>
     * 直接访问 {@link SecurityContextHolder#getContext()}，获取存储在当前线程 {@code ThreadLocal} 中的认证令牌。
     *
     * @return Spring Security 认证信息接口
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户 ID (Safe User ID Retrieval)
     * <p>
     * <b>业务场景：</b>
     * 常用于 MyBatisPlus 的自动填充 (如 {@code create_by}, {@code update_by}) 或业务数据的归属权校验。
     *
     * @return 用户主键 ID，若未登录则返回 null
     */
    public Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * 获取当前用户名 (Safe Username Retrieval)
     * <p>
     * <b>业务场景：</b>
     * 常用于日志审计 (Audit Log) 或界面展示。
     * <p>
     * <b>兜底策略：</b>
     * 若未获取到用户信息（如匿名访问接口），返回默认值 {@code "Anonymous"}，确保日志记录不报错。
     *
     * @return 用户账号或 "Anonymous"
     */
    public String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : "Anonymous";
    }
}