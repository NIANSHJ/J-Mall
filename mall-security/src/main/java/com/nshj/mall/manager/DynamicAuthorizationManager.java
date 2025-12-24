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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
 * 它是 "零信任" 安全架构中的 PDP (Policy Decision Point, 策略判定点)。
 * 接管 Spring Security 默认的静态配置，实现基于数据库的、细粒度的 RESTful API 访问控制。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 * <li><b>高性能设计 (High Performance):</b> 采用 <b>JVM 堆内缓存 -> Redis -> DB</b> 的三级缓存架构。鉴权过程完全在内存中完成，无网络 I/O 开销，判定耗时微秒级。</li>
 * <li><b>RESTful 适配:</b> 引入 {@code METHOD:URL} 复合键设计 (如 {@code POST:/users} vs {@code GET:/users})，解决同 URL 不同操作的权限冲突。</li>
 * <li><b>无锁并发:</b> 利用 {@code volatile} 关键字实现 Copy-On-Write 思想，在规则刷新时避免读写锁竞争。</li>
 * </ul>
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
    private final RedissonClient redissonClient;

    /**
     * 本地权限规则快照 (Level 1 Cache)
     * <p>
     * <b>数据结构：</b> 有序哈希表 (LinkedHashMap)。
     * <br>
     * <b>Key 定义：</b> {@code METHOD:URL} (例如: {@code GET:/system/user/**})
     * <br>
     * <b>Value 定义：</b> 权限标识符 (例如: {@code system:user:list})
     * <br>
     * <b>并发策略：</b> 使用 {@code volatile} 保证可见性。每次刷新时直接替换引用，实现无锁读。
     * <br>
     * <b>排序策略：</b> <font color="red">至关重要</font>。必须按 URL 路径长度倒序排列，确保 "精确路径" (Longest Match) 优先于 "通配符路径" 被匹配。
     */
    private volatile Map<String, String> localCache = Collections.emptyMap();

    /**
     * 执行鉴权裁决 (Policy Enforcement)
     * <p>
     * 对每一个进入系统的 HTTP 请求进行实时判定。
     *
     * @param authentication 当前安全主体 (User Principal)，包含用户持有的权限列表
     * @param context        请求上下文，包含 Request 对象
     * @return 鉴权决策结果 {@link AuthorizationDecision} (Granted/Denied)
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        String requestPath = context.getRequest().getRequestURI();
        String requestMethod = context.getRequest().getMethod().toUpperCase();

        // Step 1: 捕获内存快照 (Reference Capture)
        // 将 volatile 变量赋值给方法栈内的局部变量。
        // 即使并发线程此刻刷新了规则 (修改了 localCache 引用)，当前请求依然基于旧快照安全完成，避免 ConcurrentModificationException。
        Map<String, String> currentRules = this.localCache;
        String neededPerm = null;

        // Step 2: 规则匹配 (Pattern Matching)
        // 遍历有序规则表。由于规则已按 URL 长度倒序，一旦匹配成功即为 "最佳匹配规则"。
        for (Map.Entry<String, String> entry : currentRules.entrySet()) {
            String ruleKey = entry.getKey();

            // 解析复合 Key: "POST:/system/user" -> method="POST", path="/system/user"
            int splitIndex = ruleKey.indexOf(AuthConstants.KEY_SEPARATOR);
            if (splitIndex == -1) continue;

            String ruleMethod = ruleKey.substring(0, splitIndex);
            String rulePath = ruleKey.substring(splitIndex + 1);

            // 核心匹配逻辑：
            // 1. 路径匹配 (使用 AntPathMatcher，支持 ** 通配符)
            // 2. 动词匹配 (规则为 ALL 或与请求 Method 完全一致)
            if (pathMatcher.match(rulePath, requestPath)) {
                if (AuthConstants.METHOD_ALL.equals(ruleMethod) || ruleMethod.equals(requestMethod)) {
                    neededPerm = entry.getValue();
                    break; // 贪婪匹配：命中即停止，不再继续查找
                }
            }
        }

        // Step 3: 默认策略 (Default Policy)
        // 场景：请求的 URL 未配置任何显式权限规则。
        // 策略：采取 "Authenticated Voted" —— 只要用户已登录即可访问 (白名单模式需在此处改为 false)。
        if (neededPerm == null) {
            return new AuthorizationDecision(authentication.get().isAuthenticated());
        }

        // Step 4: 权限比对 (Authority Voting)
        // 检查用户持有的 GrantedAuthority 集合中是否包含该资源所需的权限标识。
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
     * 定时同步任务
     * <p>
     * 兜底策略：每小时同步一次，确保数据最终一致性。
     */
    @PostConstruct
    @Scheduled(cron = AuthConstants.AUTH_FLUSH_CRON)
    public void scheduledRefresh() {
        refreshRules(false); // 定时任务不需要强制查库，优先读 Redis
    }

    /**
     * 刷新权限规则 (Hot Reload)
     * <p>
     * 支持两种模式：
     * 1. 强制查库 (forceDb=true): 用于后台管理修改权限后，通过 MQ 广播触发的实时刷新。
     * 2. 优先缓存 (forceDb=false): 用于定时任务或应用启动。
     *
     * @param forceDb 是否强制跳过 Redis 直接查询数据库
     */
    public void refreshRules(boolean forceDb) {
        try {
            // 1. 加载原始数据 (Cache-Aside 模式: Redis -> DB)
            Map<String, String> rawRules = this.loadUrlPermRules(forceDb);

            // 2. 预处理：拓扑排序 (Topological Sorting)
            // 目的：确保 "/api/user/add" (特异性高) 排在 "/api/user/**" (特异性低) 之前。
            // 算法：即使 Key 包含 Method 前缀，依然仅根据 URL 部分的长度进行倒序排序。
            Map<String, String> sortedRules = rawRules.entrySet().stream()
                    .sorted((e1, e2) -> {
                        String url1 = extractUrlFromKey(e1.getKey());
                        String url2 = extractUrlFromKey(e2.getKey());
                        return url2.length() - url1.length(); // 长度倒序
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldVal, newVal) -> oldVal, // 合并策略：Key 不应重复，若重复保留旧值
                            LinkedHashMap::new          // 必须使用 LinkedHashMap 保持排序顺序
                    ));

            // 3. 原子切换 (Atomic Swap)
            // 修改 volatile 引用，瞬间完成规则更新，无锁且线程安全。
            this.localCache = sortedRules;

            log.debug("动态权限规则刷新完毕，当前生效规则数: {}", sortedRules.size());
        } catch (Exception e) {
            // 启动保护：若首次加载失败，禁止应用启动，避免裸奔风险
            if (MapUtil.isEmpty(this.localCache)) {
                log.error("致命错误：权限规则初始化失败，应用启动中止！", e);
                throw new RuntimeException("权限规则初始化失败", e);
            }

            // 运行期容错：若刷新失败，静默保持上一版本的规则继续服务
            log.error("动态权限规则刷新异常，系统维持旧规则运行", e);
        }
    }

    /**
     * 加载全量权限规则 (Data Loader)
     * <p>
     * 实现标准的 Cache-Aside 模式：优先读 Redis，未命中查 DB 并回写。
     * 同时完成数据的 ETL (Extract-Transform-Load) 清洗工作。
     *
     * @param forceDb 是否强制回源查询数据库
     * @return 格式化后的权限映射表 (Key format: METHOD:URL)
     */
    private Map<String, String> loadUrlPermRules(boolean forceDb) {
        // 第一次检查 (Check 1): 无锁快速判断
        if (!forceDb) {
            Map<String, String> cachedMap = redisCache.getCacheMap(RedisConstants.SYS_AUTH_RULES_KEY);
            if (MapUtil.isNotEmpty(cachedMap)) {
                return cachedMap;
            }
        }

        // 准备分布式锁
        String lockKey = RedisConstants.LOCK_PREFIX + RedisConstants.SYS_AUTH_RULES_KEY;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            // 尝试获取锁 (Wait Time: 3s, Lease Time: 10s)
            isLocked = lock.tryLock(RedisConstants.LOCK_WAIT_SECONDS, RedisConstants.LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

            if (isLocked) {
                // 获取锁成功 -> 进入临界区

                // 第二次检查 (Check 2): 防止重复查询
                // 场景：Thread A 和 B 同时发现缓存为空，A 拿锁查库回写缓存后释放，B 拿到锁应再次检查缓存，避免重复查库。
                if (!forceDb) {
                    Map<String, String> doubleCheckMap = redisCache.getCacheMap(RedisConstants.SYS_AUTH_RULES_KEY);
                    if (MapUtil.isNotEmpty(doubleCheckMap)) {
                        return doubleCheckMap;
                    }
                }

                // --- 临界区开始：查询数据库 ---
                log.info("缓存未命中或强制刷新，正在查询数据库...");
                List<SysMenu> menuList = sysMenuService.getMenuRules();

                // ETL 数据清洗
                Map<String, String> dbMap = new HashMap<>();
                for (SysMenu menu : menuList) {
                    if (StringUtils.hasText(menu.getApiPath()) && StringUtils.hasText(menu.getPerms())) {
                        String method = StringUtils.hasText(menu.getRequestMethod())
                                ? menu.getRequestMethod().toUpperCase()
                                : AuthConstants.METHOD_ALL;
                        String key = method + AuthConstants.KEY_SEPARATOR + menu.getApiPath();
                        dbMap.put(key, menu.getPerms());
                    }
                }

                // 回写 Redis (设置 TTL 防止僵尸数据)
                if (!dbMap.isEmpty()) {
                    redisCache.executePipeline(ops -> {
                        ops.delete(RedisConstants.SYS_AUTH_RULES_KEY);
                        ops.opsForHash().putAll(RedisConstants.SYS_AUTH_RULES_KEY, dbMap);
                        ops.expire(RedisConstants.SYS_AUTH_RULES_KEY, AuthConstants.AUTH_EXPIRATION, TimeUnit.HOURS);
                    });
                }
                // --- 临界区结束 ---

                return dbMap;

            } else {
                // 获取锁失败 (说明有其他节点正在查库)
                log.warn("获取权限加载锁失败，正在等待其他节点构建缓存...");

                // 稍微休眠，等待持有锁的线程完成缓存构建
                Thread.sleep(RedisConstants.LOCK_FAILURE_SLEEP_MILLIS);

                // 再次尝试读取(此时缓存应该已经有了)
                return redisCache.getCacheMap(RedisConstants.SYS_AUTH_RULES_KEY);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("权限刷新线程被中断", e);
        } catch (Exception e) {
            log.error("加载权限数据异常", e);
            throw new RuntimeException("加载权限数据失败", e);
        } finally {
            // 释放锁 (只有持有锁的线程才能释放)
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 辅助工具：从复合 Key 中提取 URL 路径部分
     *
     * @param key 格式如 "POST:/user/add"
     * @return 提取出的 URL，如 "/user/add"
     */
    private String extractUrlFromKey(String key) {
        int index = key.indexOf(AuthConstants.KEY_SEPARATOR);
        if (index != -1) {
            return key.substring(index + 1);
        }
        return key;
    }
}