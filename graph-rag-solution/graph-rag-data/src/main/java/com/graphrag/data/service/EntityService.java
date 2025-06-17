package com.graphrag.data.service;

import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 实体服务
 */
@Service
@Transactional
public class EntityService {

    private static final Logger logger = LoggerFactory.getLogger(EntityService.class);

    @Autowired
    private EntityRepository entityRepository;

    /**
     * 保存实体
     */
    public EntityNode saveEntity(EntityNode entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        EntityNode saved = entityRepository.save(entity);
        logger.info("保存实体成功，ID: {}, 名称: {}, 类型: {}", saved.getId(), saved.getName(), saved.getType());
        return saved;
    }

    /**
     * 根据ID查找实体
     */
    public Optional<EntityNode> findById(Long id) {
        return entityRepository.findById(id);
    }

    /**
     * 根据名称查找实体
     */
    public Optional<EntityNode> findByName(String name) {
        return entityRepository.findByName(name);
    }

    /**
     * 根据类型查找实体
     */
    public List<EntityNode> findByType(String type) {
        return entityRepository.findByType(type);
    }

    /**
     * 根据名称和类型查找实体
     */
    public Optional<EntityNode> findByNameAndType(String name, String type) {
        return entityRepository.findByNameAndType(name, type);
    }

    /**
     * 查找或创建实体
     */
    public EntityNode findOrCreateEntity(String name, String type) {
        Optional<EntityNode> existing = findByNameAndType(name, type);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        EntityNode newEntity = new EntityNode(name, type);
        return saveEntity(newEntity);
    }

    /**
     * 查找与指定实体相关的实体
     */
    public List<EntityNode> findRelatedEntities(String entityName) {
        return entityRepository.findRelatedEntities(entityName);
    }

    /**
     * 查找与指定实体相关的实体（指定关系类型）
     */
    public List<EntityNode> findRelatedEntitiesByType(String entityName, String relationType) {
        return entityRepository.findRelatedEntitiesByType(entityName, relationType);
    }

    /**
     * 查找两个实体之间的最短路径
     */
    public List<Object> findShortestPath(String entity1, String entity2) {
        return entityRepository.findShortestPath(entity1, entity2);
    }

    /**
     * 向量相似性搜索实体
     */
    public List<EntityNode> findSimilarEntities(List<Double> queryEmbedding, Double threshold, Integer limit) {
        if (threshold == null) threshold = 0.7;
        if (limit == null) limit = 10;
        return entityRepository.findSimilarEntities(queryEmbedding, threshold, limit);
    }

    /**
     * 查找最重要的实体（根据连接度）
     */
    public List<EntityNode> findMostConnectedEntities(Integer limit) {
        if (limit == null) limit = 10;
        return entityRepository.findMostConnectedEntities(limit);
    }

    /**
     * 更新实体嵌入向量
     */
    public EntityNode updateEmbedding(Long id, List<Double> embedding) {
        Optional<EntityNode> optionalEntity = entityRepository.findById(id);
        if (optionalEntity.isPresent()) {
            EntityNode entity = optionalEntity.get();
            entity.setEmbedding(embedding);
            entity.setUpdatedAt(LocalDateTime.now());
            return entityRepository.save(entity);
        }
        throw new RuntimeException("实体不存在，ID: " + id);
    }

    /**
     * 删除实体
     */
    public void deleteEntity(Long id) {
        entityRepository.deleteById(id);
        logger.info("删除实体成功，ID: {}", id);
    }

    /**
     * 获取所有实体
     */
    public List<EntityNode> findAll() {
        return entityRepository.findAll();
    }

    /**
     * 批量保存实体
     */
    public List<EntityNode> saveAll(List<EntityNode> entities) {
        entities.forEach(entity -> entity.setUpdatedAt(LocalDateTime.now()));
        List<EntityNode> saved = entityRepository.saveAll(entities);
        logger.info("批量保存实体成功，数量: {}", saved.size());
        return saved;
    }
}

