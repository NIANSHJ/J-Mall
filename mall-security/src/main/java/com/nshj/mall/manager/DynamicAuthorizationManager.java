package com.nshj.mall.manager;

import cn.hutool.core.map.MapUtil;
import com.nshj.mall.constant.AuthConstants;
import com.nshj.mall.constant.RedisConstants;
import com.nshj.mall.entity.SysMenu;
import com.nshj.mall.service.SysMenuService;
import com.nshj.mall.utils.RedisCache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 动态鉴权决策管理器 (Dynamic RBAC Decision Manager)
 * <p>
 * <b>架构定位：</b>
 * 接管 Spring Security 默认的静态权限配置，实现基于数据库的动态 RESTful API 访问控制。
 * 它是 "零信任" 安全架构中负责 PDP (Policy Decision Point) 策略判定的核心组件。
 * <p>
 * <b>性能设计 (High Performance):</b>
 * 采用 <b>"三级缓存架构" (Local Memory -> Redis -> DB)</b>。
 * 鉴权过程全程在 JVM 堆内存中完成 (Local Map)，完全避免了 IO 开销，确保单次鉴权耗时控制在微秒级。
 * <p>
 * <b>数据结构设计 (Data Structure):</b>
 * 内存映射表 {@code localCache} 的 Key 采用复合结构 <b>"METHOD:URL"</b> (如 {@code POST:/user/**})。
 * 解决了 RESTful 风格下同一 URL 对应不同操作权限的冲突问题。
 *
 * @author nshj
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final RedisCache redisCache;
    private final SysMenuService sysMenuService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 分隔符：用于连接 HTTP 方法与 URL 路径
     */
    private static final String KEY_SEPARATOR = ":";

    /**
     * 通用方法标识：当数据库未配置请求方式时，默认为 ALL，匹配所有 Method
     */
    private static final String METHOD_ALL = "ALL";

    /**
     * 本地权限规则快照 (Level 1 Cache)
     * <p>
     * <b>Key 格式：</b> {@code METHOD:URL} (例: {@code GET:/system/user})
     * <br>
     * <b>Value 格式：</b> 权限标识 (例: {@code system:user:list})
     * <br>
     * <b>排序策略：</b> 按 URL 部分的长度倒序排列，确保精确路径优先匹配。
     */
    private volatile Map<String, String> localCache = Collections.emptyMap();

    /**
     * 执行鉴权裁决 (Policy Enforcement)
     * <p>
     * 对每一个进入系统的 HTTP 请求进行实时判定。
     * 支持 RESTful 风格的 Method 区分。
     *
     * @param authentication 当前安全主体 (User Principal)
     * @param context        请求上下文 (包含 Request/Response)
     * @return 鉴权结果 {@link AuthorizationDecision}
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        String requestPath = context.getRequest().getRequestURI();
        String requestMethod = context.getRequest().getMethod().toUpperCase();

        // Step 1: 捕获内存快照 (Reference Capture)
        // 将 volatile 变量赋值给局部变量，建立"方法栈帧内的引用"，防止鉴权过程中 cache 被后台线程置换。
        Map<String, String> currentRules = this.localCache;
        String neededPerm = null;

        // Step 2: 规则匹配 (Pattern Matching)
        // 遍历有序规则表。规则已按 URL 长度倒序，因此一旦匹配成功即为"最佳匹配"。
        for (Map.Entry<String, String> entry : currentRules.entrySet()) {
            String ruleKey = entry.getKey();

            // 解析复合 Key: "POST:/system/user" -> method="POST", path="/system/user"
            int splitIndex = ruleKey.indexOf(KEY_SEPARATOR);
            if (splitIndex == -1) continue;

            String ruleMethod = ruleKey.substring(0, splitIndex);
            String rulePath = ruleKey.substring(splitIndex + 1);

            // 核心匹配逻辑：
            // 1. 路径匹配 (AntPathMatcher 支持 ** 通配符)
            // 2. 方法匹配 (规则方法为 ALL 或与请求方法完全一致)
            if (pathMatcher.match(rulePath, requestPath)) {
                if (METHOD_ALL.equals(ruleMethod) || ruleMethod.equals(requestMethod)) {
                    neededPerm = entry.getValue();
                    break; // 贪婪匹配：找到即停止
                }
            }
        }

        // Step 3: 默认策略 (Default Policy)
        // 场景：URL 未配置任何权限规则。
        // 策略：默认采取 "Authenticated Voted" —— 只要登录即可访问。
        if (neededPerm == null) {
            return new AuthorizationDecision(authentication.get().isAuthenticated());
        }

        // Step 4: 权限比对 (Authority Voting)
        // 检查用户持有的 GrantedAuthority 集合中是否包含该 URL 所需的权限标识。
        Collection<? extends GrantedAuthority> authorities = authentication.get().getAuthorities();
        String finalNeededPerm = neededPerm;

        boolean hasPermission = authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(finalNeededPerm));

        return new AuthorizationDecision(hasPermission);
    }

    // ==========================================
    // 数据同步与生命周期管理
    // ==========================================

    /**
     * 刷新权限规则 (Hot Reload)
     * <p>
     * <b>触发机制：</b>
     * 1. {@code @PostConstruct}: 应用冷启动初始化。
     * 2. {@code @Scheduled}: 周期性(1h) 自动同步，保证最终一致性。
     */
    @PostConstruct
    @Scheduled(cron = AuthConstants.AUTH_FLUSH_CRON)
    public void refreshRules() {
        try {
            // 1. 获取原始数据 (DB/Redis)，此时 Key 已是 "METHOD:URL" 格式
            Map<String, String> rawRules = this.loadUrlPermRules();

            // 2. 预处理：排序 (Sorting)
            // 核心逻辑：即使 Key 包含了 Method，排序依然依据 URL 的长度。
            // 目的：确保 /user/add (精确匹配) 在 /user/** (通配符匹配) 之前被遍历到。
            Map<String, String> sortedRules = rawRules.entrySet().stream()
                    .sorted((e1, e2) -> {
                        String url1 = extractUrlFromKey(e1.getKey());
                        String url2 = extractUrlFromKey(e2.getKey());
                        return url2.length() - url1.length(); // 长度倒序
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldVal, newVal) -> oldVal, // 理论上 Key 不会重复
                            LinkedHashMap::new          // 必须使用 LinkedHashMap 保持排序结果
                    ));

            // 3. 原子切换 (Atomic Swap)
            // 修改 volatile 引用，瞬间完成规则更新。
            this.localCache = sortedRules;

            log.debug("动态权限规则刷新完毕，当前生效规则数: {}", sortedRules.size());
        } catch (Exception e) {
            if (MapUtil.isEmpty(this.localCache)) {
                log.error("启动阶段权限加载失败，应用将拒绝启动！", e);
                throw new RuntimeException("权限规则初始化失败，系统启动中止", e);
            }

            // 容错策略：刷新失败时，系统静默保持上一版本的规则继续运行，避免服务中断。
            log.error("动态权限规则刷新异常", e);
        }
    }

    /**
     * 加载原始规则数据 (Data Loader)
     * <p>
     * 采用 Cache-Aside 模式：优先读 Redis，未命中查 DB 并回写。
     * 并在加载过程中完成 Key 的格式化构造。
     *
     * @return 无序的权限映射 Map (Key format: METHOD:URL)
     */
    private Map<String, String> loadUrlPermRules() {
        // 1. 查询 Redis 分布式缓存
        Map<String, String> cachedMap = redisCache.getCacheMap(RedisConstants.SYS_AUTH_RULES_KEY);
        if (MapUtil.isNotEmpty(cachedMap)) {
            return cachedMap;
        }

        // 2. 缓存击穿，查询数据库
        List<SysMenu> menuList = sysMenuService.getMenuRules();

        // 3. 数据清洗与 Key 构造 (ETL)
        // 过滤掉无效数据，并组装 "METHOD:URL" 复合 Key
        Map<String, String> dbMap = new HashMap<>();
        for (SysMenu menu : menuList) {
            if (StringUtils.hasText(menu.getApiPath()) && StringUtils.hasText(menu.getPerms())) {

                // 处理 Request Method：若为空则视为 ALL
                String method = StringUtils.hasText(menu.getRequestMethod())
                        ? menu.getRequestMethod().toUpperCase()
                        : METHOD_ALL;

                // 构造唯一 Key: "GET:/system/user"
                String key = method + KEY_SEPARATOR + menu.getApiPath();

                dbMap.put(key, menu.getPerms());
            }
        }

        // 4. 重建缓存 (TTL: 1小时)
        if (!dbMap.isEmpty()) {
            redisCache.setCacheMap(RedisConstants.SYS_AUTH_RULES_KEY, dbMap);
            redisCache.expire(RedisConstants.SYS_AUTH_RULES_KEY, AuthConstants.AUTH_EXPIRATION, TimeUnit.HOURS);
        }

        return dbMap;
    }

    /**
     * 辅助方法：从复合 Key 中提取 URL 部分
     *
     * @param key 格式如 "POST:/user/add"
     * @return 提取出的 URL，如 "/user/add"
     */
    private String extractUrlFromKey(String key) {
        int index = key.indexOf(KEY_SEPARATOR);
        if (index != -1) {
            return key.substring(index + 1);
        }
        return key;
    }
}