package com.graphrag.core.algorithm;

import com.graphrag.data.service.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级图遍历算法
 */
@Component
public class GraphTraversalAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GraphTraversalAlgorithm.class);

    @Autowired
    private GraphService graphService;

    /**
     * 多跳实体检索
     */
    public List<Map<String, Object>> multiHopEntityRetrieval(String startEntity, int maxHops, int maxResults) {
        logger.debug("开始多跳实体检索，起始实体: {}, 最大跳数: {}", startEntity, maxHops);

        String cypher = """
                MATCH path = (start:Entity {name: $startEntity})-[*1..%d]-(end:Entity)
                WHERE start <> end
                WITH path, end, length(path) as pathLength
                ORDER BY pathLength, end.name
                LIMIT $maxResults
                RETURN end.name as entityName, end.type as entityType, end.description as description, 
                       pathLength, [node in nodes(path) | node.name] as pathNodes,
                       [rel in relationships(path) | type(rel)] as relationshipTypes
                """.formatted(maxHops);

        Map<String, Object> parameters = Map.of(
                "startEntity", startEntity,
                "maxResults", maxResults
        );

        List<Map<String, Object>> results = graphService.executeCypher(cypher, parameters);
        logger.info("多跳检索完成，找到 {} 个相关实体", results.size());
        return results;
    }

    /**
     * 路径查找算法
     */
    public List<Map<String, Object>> findPaths(String entity1, String entity2, int maxPathLength) {
        logger.debug("查找路径: {} -> {}, 最大路径长度: {}", entity1, entity2, maxPathLength);

        String cypher = """
                MATCH path = (e1:Entity {name: $entity1})-[*1..%d]-(e2:Entity {name: $entity2})
                WITH path, length(path) as pathLength
                ORDER BY pathLength
                LIMIT 10
                RETURN [node in nodes(path) | {name: node.name, type: node.type}] as nodes,
                       [rel in relationships(path) | {type: type(rel), description: rel.description}] as relationships,
                       pathLength
                """.formatted(maxPathLength);

        Map<String, Object> parameters = Map.of(
                "entity1", entity1,
                "entity2", entity2
        );

        return graphService.executeCypher(cypher, parameters);
    }

    /**
     * 社区检测算法
     */
    public List<Map<String, Object>> detectCommunities(List<String> entityNames, double threshold) {
        logger.debug("开始社区检测，实体数量: {}, 阈值: {}", entityNames.size(), threshold);

        String cypher = """
                MATCH (e1:Entity)-[r]-(e2:Entity)
                WHERE e1.name IN $entityNames AND e2.name IN $entityNames
                WITH e1, e2, r.weight as weight
                WHERE weight >= $threshold
                RETURN e1.name as entity1, e2.name as entity2, weight
                ORDER BY weight DESC
                """;

        Map<String, Object> parameters = Map.of(
                "entityNames", entityNames,
                "threshold", threshold
        );

        return graphService.executeCypher(cypher, parameters);
    }

    /**
     * 中心性分析
     */
    public List<Map<String, Object>> calculateCentrality(List<String> entityNames, String centralityType) {
        logger.debug("计算中心性，类型: {}, 实体数量: {}", centralityType, entityNames.size());

        String cypher;
        switch (centralityType.toLowerCase()) {
            case "degree":
                cypher = """
                        MATCH (e:Entity)
                        WHERE e.name IN $entityNames
                        WITH e, size((e)-[]-()) as degree
                        RETURN e.name as entityName, e.type as entityType, degree
                        ORDER BY degree DESC
                        """;
                break;
            case "betweenness":
                cypher = """
                        MATCH (e:Entity)
                        WHERE e.name IN $entityNames
                        WITH e
                        MATCH path = allShortestPaths((start:Entity)-[*]-(end:Entity))
                        WHERE start.name IN $entityNames AND end.name IN $entityNames 
                              AND start <> end AND e IN nodes(path)
                        WITH e, count(path) as betweenness
                        RETURN e.name as entityName, e.type as entityType, betweenness
                        ORDER BY betweenness DESC
                        """;
                break;
            case "closeness":
                cypher = """
                        MATCH (e:Entity)
                        WHERE e.name IN $entityNames
                        WITH e
                        MATCH (e)-[*]-(other:Entity)
                        WHERE other.name IN $entityNames AND e <> other
                        WITH e, avg(length(shortestPath((e)-[*]-(other)))) as avgDistance
                        RETURN e.name as entityName, e.type as entityType, 
                               CASE WHEN avgDistance > 0 THEN 1.0/avgDistance ELSE 0 END as closeness
                        ORDER BY closeness DESC
                        """;
                break;
            default:
                throw new IllegalArgumentException("不支持的中心性类型: " + centralityType);
        }

        Map<String, Object> parameters = Map.of("entityNames", entityNames);
        return graphService.executeCypher(cypher, parameters);
    }

    /**
     * 子图提取
     */
    public Map<String, Object> extractSubgraph(List<String> entityNames, int maxDepth) {
        logger.debug("提取子图，实体数量: {}, 最大深度: {}", entityNames.size(), maxDepth);

        // 获取节点
        String nodesCypher = """
                MATCH (e:Entity)
                WHERE e.name IN $entityNames
                OPTIONAL MATCH (e)-[*1..%d]-(connected:Entity)
                WITH collect(DISTINCT e) + collect(DISTINCT connected) as allNodes
                UNWIND allNodes as node
                RETURN DISTINCT node.name as name, node.type as type, node.description as description
                """.formatted(maxDepth);

        // 获取关系
        String relationshipsCypher = """
                MATCH (e1:Entity)-[r]-(e2:Entity)
                WHERE e1.name IN $entityNames
                OPTIONAL MATCH (e1)-[*1..%d]-(intermediate:Entity)-[r2]-(e2)
                WHERE intermediate.name IN $entityNames OR e2.name IN $entityNames
                RETURN DISTINCT e1.name as source, e2.name as target, type(r) as relationshipType, 
                       r.description as description, r.weight as weight
                """.formatted(maxDepth);

        Map<String, Object> parameters = Map.of("entityNames", entityNames);

        List<Map<String, Object>> nodes = graphService.executeCypher(nodesCypher, parameters);
        List<Map<String, Object>> relationships = graphService.executeCypher(relationshipsCypher, parameters);

        return Map.of(
                "nodes", nodes,
                "relationships", relationships,
                "nodeCount", nodes.size(),
                "relationshipCount", relationships.size()
        );
    }

    /**
     * 相似实体聚类
     */
    public List<List<String>> clusterSimilarEntities(List<String> entityNames, double similarityThreshold) {
        logger.debug("相似实体聚类，实体数量: {}, 相似度阈值: {}", entityNames.size(), similarityThreshold);

        // 获取实体的嵌入向量
        String cypher = """
                MATCH (e:Entity)
                WHERE e.name IN $entityNames AND e.embedding IS NOT NULL
                RETURN e.name as name, e.embedding as embedding
                """;

        Map<String, Object> parameters = Map.of("entityNames", entityNames);
        List<Map<String, Object>> entityEmbeddings = graphService.executeCypher(cypher, parameters);

        // 简单的聚类算法（基于相似度阈值）
        List<List<String>> clusters = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (Map<String, Object> entity : entityEmbeddings) {
            String entityName = (String) entity.get("name");
            if (processed.contains(entityName)) {
                continue;
            }

            List<String> cluster = new ArrayList<>();
            cluster.add(entityName);
            processed.add(entityName);

            @SuppressWarnings("unchecked")
            List<Double> embedding1 = (List<Double>) entity.get("embedding");

            for (Map<String, Object> otherEntity : entityEmbeddings) {
                String otherName = (String) otherEntity.get("name");
                if (processed.contains(otherName)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Double> embedding2 = (List<Double>) otherEntity.get("embedding");

                double similarity = calculateCosineSimilarity(embedding1, embedding2);
                if (similarity >= similarityThreshold) {
                    cluster.add(otherName);
                    processed.add(otherName);
                }
            }

            if (cluster.size() > 1) {
                clusters.add(cluster);
            }
        }

        logger.info("聚类完成，生成 {} 个聚类", clusters.size());
        return clusters;
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 图模式匹配
     */
    public List<Map<String, Object>> patternMatching(String pattern, Map<String, Object> parameters) {
        logger.debug("执行图模式匹配: {}", pattern);

        try {
            return graphService.executeCypher(pattern, parameters);
        } catch (Exception e) {
            logger.error("图模式匹配失败: {}", pattern, e);
            return List.of();
        }
    }

    /**
     * 动态图遍历
     */
    public List<Map<String, Object>> dynamicTraversal(String startEntity, List<String> relationshipTypes, 
                                                     int maxDepth, int maxResults) {
        logger.debug("动态图遍历，起始实体: {}, 关系类型: {}", startEntity, relationshipTypes);

        String relationshipFilter = relationshipTypes.isEmpty() ? "" : 
                "WHERE type(r) IN " + relationshipTypes.toString().replace("[", "['").replace("]", "']").replace(", ", "', '");

        String cypher = """
                MATCH path = (start:Entity {name: $startEntity})-[r*1..%d]-(end:Entity)
                %s
                WITH path, end, length(path) as depth
                ORDER BY depth, end.name
                LIMIT $maxResults
                RETURN end.name as entityName, end.type as entityType, end.description as description,
                       depth, [node in nodes(path) | node.name] as pathNodes
                """.formatted(maxDepth, relationshipFilter);

        Map<String, Object> params = Map.of(
                "startEntity", startEntity,
                "maxResults", maxResults
        );

        return graphService.executeCypher(cypher, params);
    }
}

