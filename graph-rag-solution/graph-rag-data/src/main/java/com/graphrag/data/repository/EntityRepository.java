package com.graphrag.data.repository;

import com.graphrag.data.entity.EntityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 实体节点仓库接口
 */
@Repository
public interface EntityRepository extends Neo4jRepository<EntityNode, Long> {

    /**
     * 根据名称查找实体
     */
    Optional<EntityNode> findByName(String name);

    /**
     * 根据类型查找实体
     */
    List<EntityNode> findByType(String type);

    /**
     * 根据名称和类型查找实体
     */
    Optional<EntityNode> findByNameAndType(String name, String type);

    /**
     * 查找与指定实体相关的实体（一跳关系）
     */
    @Query("MATCH (e1:Entity)-[r]-(e2:Entity) " +
           "WHERE e1.name = $entityName " +
           "RETURN e2")
    List<EntityNode> findRelatedEntities(@Param("entityName") String entityName);

    /**
     * 查找与指定实体相关的实体（指定关系类型）
     */
    @Query("MATCH (e1:Entity)-[r:$relationType]-(e2:Entity) " +
           "WHERE e1.name = $entityName " +
           "RETURN e2")
    List<EntityNode> findRelatedEntitiesByType(
        @Param("entityName") String entityName,
        @Param("relationType") String relationType
    );

    /**
     * 查找两个实体之间的最短路径
     */
    @Query("MATCH path = shortestPath((e1:Entity)-[*]-(e2:Entity)) " +
           "WHERE e1.name = $entity1 AND e2.name = $entity2 " +
           "RETURN path")
    List<Object> findShortestPath(
        @Param("entity1") String entity1,
        @Param("entity2") String entity2
    );

    /**
     * 向量相似性搜索实体
     */
    @Query("MATCH (e:Entity) " +
           "WHERE e.embedding IS NOT NULL " +
           "WITH e, gds.similarity.cosine(e.embedding, $queryEmbedding) AS similarity " +
           "WHERE similarity > $threshold " +
           "RETURN e ORDER BY similarity DESC LIMIT $limit")
    List<EntityNode> findSimilarEntities(
        @Param("queryEmbedding") List<Double> queryEmbedding,
        @Param("threshold") Double threshold,
        @Param("limit") Integer limit
    );

    /**
     * 根据度中心性查找重要实体
     */
    @Query("MATCH (e:Entity) " +
           "WITH e, size((e)-[]-()) AS degree " +
           "RETURN e ORDER BY degree DESC LIMIT $limit")
    List<EntityNode> findMostConnectedEntities(@Param("limit") Integer limit);
}

