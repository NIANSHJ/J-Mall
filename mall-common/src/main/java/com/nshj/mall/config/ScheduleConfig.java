package com.nshj.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 分布式定时任务调度配置 (Scheduled Task Configuration)
 * <p>
 * <b>架构定位：</b>
 * 属于系统基础设施层，负责全局定时任务的线程资源管理。
 * <p>
 * <b>核心痛点解决：</b>
 * Spring Boot 默认的 {@code @Scheduled} 使用单线程调度器。若某个任务执行耗时过长或阻塞，
 * 会导致后续任务堆积甚至跳过执行。本配置通过引入<b>线程池模型</b>，实现多任务的并行调度与隔离。
 *
 * @author nshj
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    /**
     * 构建自定义任务调度器 Bean
     * <p>
     * <b>配置策略：</b>
     * <ul>
     * <li><b>并发度 (Concurrency):</b> 设置核心线程数为 5，支持多个定时任务同时执行，互不干扰。</li>
     * <li><b>可观测性 (Observability):</b> 自定义线程名称前缀 {@code Schedule-}，便于在 ELK 日志或堆栈追踪中快速定位问题。</li>
     * <li><b>优雅停机 (Graceful Shutdown):</b> 应用关闭时，最大等待 60 秒，确保正在执行的原子性任务（如数据同步、报表生成）能够安全完成，避免数据损坏。</li>
     * </ul>
     *
     * @return 增强型线程池调度器实例
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(5);

        // 线程名称前缀，方便在日志里排查问题 (会显示 Schedule-1, Schedule-2...)
        scheduler.setThreadNamePrefix("Schedule-");

        // 优雅停机：等待任务执行完再关闭
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        return scheduler;
    }
}