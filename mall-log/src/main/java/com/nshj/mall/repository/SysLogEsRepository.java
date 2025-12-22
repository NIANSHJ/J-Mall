package com.nshj.mall.repository;

import com.nshj.mall.entity.AuditLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统日志搜索引擎仓库接口 (System Log Elasticsearch Repository)
 * <p>
 * <b>架构定位：</b>
 * 位于搜索数据持久层 (Search Persistence Layer)。
 * 它是应用服务与 Elasticsearch 集群之间的交互契约，屏蔽了底层复杂的 RESTful API 和 JSON 序列化细节。
 * <p>
 * <b>核心职能：</b>
 * 负责 {@link AuditLog} 文档对象在 Elasticsearch 索引中的生命周期管理。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 * <li><b>标准 CRUD：</b> 继承 {@link ElasticsearchRepository}，自动获得 {@code save}, {@code saveAll}, {@code findById} 等标准方法。</li>
 * <li><b>DSL 封装：</b> 支持通过方法名约定 (Query Creation from Method Names) 快速构建查询，无需编写原生 Query DSL。</li>
 * </ul>
 *
 * @author nshj
 * @since 1.0.0
 */
@Repository
public interface SysLogEsRepository extends ElasticsearchRepository<AuditLog, String> {
}