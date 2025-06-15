package com.graphrag.data.repository;

import com.graphrag.data.entity.DocumentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档节点仓库接口
 */
@Repository
public interface DocumentRepository extends Neo4jRepository<DocumentNode, Long> {

    /**
     * 根据标题查找文档
     */
    Optional<DocumentNode> findByTitle(String title);

    /**
     * 根据来源查找文档
     */
    List<DocumentNode> findBySource(String source);

    /**
     * 根据内容关键词搜索文档
     */
    @Query("MATCH (d:Document) WHERE d.content CONTAINS $keyword RETURN d")
    List<DocumentNode> findByContentContaining(@Param("keyword") String keyword);

    /**
     * 向量相似性搜索
     */
    @Query("MATCH (d:Document) " +
           "WHERE d.embedding IS NOT NULL " +
           "WITH d, gds.similarity.cosine(d.embedding, $queryEmbedding) AS similarity " +
           "WHERE similarity > $threshold " +
           "RETURN d ORDER BY similarity DESC LIMIT $limit")
    List<DocumentNode> findSimilarDocuments(
        @Param("queryEmbedding") List<Double> queryEmbedding,
        @Param("threshold") Double threshold,
        @Param("limit") Integer limit
    );

    /**
     * 获取所有有嵌入向量的文档
     */
    @Query("MATCH (d:Document) WHERE d.embedding IS NOT NULL RETURN d")
    List<DocumentNode> findAllWithEmbedding();
}

