package com.nshj.mall.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redis 缓存操作门面工具类 (Facade Pattern)
 * <p>
 * 对 Spring 原生 {@link RedisTemplate} 进行二次封装，提供语义清晰、强类型的缓存操作 API。
 * <p>
 * <b>核心价值：</b>
 * <ul>
 * <li><b>屏蔽底层细节：</b> 隐藏 {@code opsForValue()}, {@code opsForHash()} 等繁琐的链式调用，降低业务代码复杂度。</li>
 * <li><b>类型安全：</b> 利用泛型自动处理对象序列化与反序列化，消除业务层的大量强制类型转换 (Casting)。</li>
 * <li><b>规范约束：</b> 强制统一使用 Spring 容器中配置好的序列化策略 (JSON/String)，防止不同模块间出现乱码问题。</li>
 * </ul>
 *
 * @author nshj
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存持久化对象 (无过期时间)
     * <p>
     * 存储普通的 Key-Value 结构。
     * <p>
     * <b><font color="red">内存安全警示：</font></b>
     * 此方法生成的 Key 将<b>永久驻留内存</b>，除非手动删除或被 Redis LRU 策略淘汰。
     * <ul>
     * <li><b>适用：</b> 系统字典、全局配置开关等极少变更且必须常驻内存的数据。</li>
     * <li><b>禁用：</b> 严禁用于存储用户会话、临时验证码、订单流水等动态增长的数据，否则极易引发 OOM (内存溢出) 事故。</li>
     * </ul>
     *
     * @param key   全局唯一键
     * @param value 待缓存的数据实体
     * @param <T>   数据类型泛型
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存时效性对象 (TTL Managed)
     * <p>
     * 存储 Key-Value 并绑定生命周期。过期后 Redis 将自动移除该 Key，释放内存。
     * <br><b>标准场景：</b> 用户 Token、手机验证码、防重放锁标记 (Non-Replay Nonce)。
     *
     * @param key      全局唯一键
     * @param value    待缓存的数据实体
     * @param timeout  存活时长
     * @param timeUnit 时间单位 (如 {@link TimeUnit#MINUTES})
     * @param <T>      数据类型泛型
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存对象
     * <p>
     * 读取 Key 对应的 Value，并自动反序列化为泛型 {@code T} 指定的类型。
     *
     * @param key 缓存键
     * @param <T> 期望的返回值类型
     * @return 缓存数据实体；若 Key 不存在或已过期，返回 {@code null}
     */
    public <T> T getCacheObject(final String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key 待移除的键
     * @return {@code true} 表示命令执行成功 (包含 Key 本身就不存在的情况)
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除对象
     * <p>
     * <b>性能优化：</b> 底层使用 {@code DEL key1 key2 ...} 原道命令，相比在循环中调用单删，能显著减少网络往返耗时 (RTT)。
     *
     * @param collection 待移除的键集合
     * @return 实际被删除的 Key 数量
     */
    public long deleteObject(final Collection<String> collection) {
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存哈希字典 (Hash Structure)
     * <p>
     * 使用 Redis {@code HASH} 结构存储 Map 数据。
     * <br><b>优势：</b> 相比 String 结构，Hash 存储大量小对象更节省内存，且支持对 Map 内单个字段的独立读写。
     * <br><b>场景：</b> 系统配置表、购物车数据、用户权限缓存 (URL -> Permission)。
     *
     * @param key     Hash 的主键
     * @param dataMap 待存储的 Map 数据
     * @param <K>     Map Key 类型
     * @param <V>     Map Value 类型
     */
    public <K, V> void setCacheMap(final String key, final Map<K, V> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获取完整哈希字典
     * <p>
     * 读取指定 Hash Key 下的所有 Field 和 Value (对应 Redis {@code HGETALL} 命令)。
     * <p>
     * <b><font color="red">性能警示：</font></b>
     * 如果 Hash 包含大量字段 (如 > 1000)，请<b>慎用</b>此方法。
     * Redis 是单线程模型，处理大 Key 会阻塞主线程，导致服务抖动。建议使用 {@code HSCAN} 迭代获取。
     *
     * @param key Hash 的主键
     * @param <K> Map Key 类型
     * @param <V> Map Value 类型
     * @return 完整的 Map 对象
     */
    public <K, V> Map<K, V> getCacheMap(final String key) {
        return (Map<K, V>) redisTemplate.opsForHash().entries(key);
    }

    /**
     * 设置/重置过期时间 (TTL Renewal)
     * <p>
     * 为已存在的 Key 设置存活时长。
     * <br><b>场景：</b> “续期”操作（如：用户在 Token 即将过期前，通过活跃操作触发自动续期）。
     *
     * @param key     目标键
     * @param timeout 时长
     * @param unit    单位
     * @return {@code true} 设置成功；{@code false} 若 Key 不存在或设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 通用管道批处理 (Pipeline Execution)
     * <p>
     * 利用 Redis Pipelining 技术，将多条命令打包成一次网络请求发送给服务器，并一次性接收所有结果。
     * <br><b>性能优化：</b> 相比串行执行 {@code N} 次命令，Pipeline 只需 {@code 1} 次网络往返 (RTT)，在大量写操作场景下能显著提升吞吐量。
     * <br><b>适用场景：</b> 缓存预热、组合命令操作 (如：先删 -> 再写 -> 后置过期)。
     * <p>
     * <b><font color="red">开发警示：</font></b>
     * 在 {@code actionConsumer} 的 Lambda 表达式内部，所有 Redis 命令 (如 {@code opsForValue().get()}) <b>只会返回 null</b>，
     * 因为命令仅被放入队列并未实际执行。若需获取返回值，请从本方法的返回结果 {@code List} 中按顺序读取。
     *
     * @param actionConsumer 函数式接口，用于定义具体的 Redis 操作逻辑
     * @return 管道中每个命令的执行结果列表 (按执行顺序排列)
     */
    public List<Object> executePipeline(Consumer<RedisOperations<String, Object>> actionConsumer) {
        return redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(@NonNull RedisOperations<K, V> operations) throws DataAccessException {
                actionConsumer.accept((RedisOperations<String, Object>) operations);
                return null;
            }
        });
    }
}