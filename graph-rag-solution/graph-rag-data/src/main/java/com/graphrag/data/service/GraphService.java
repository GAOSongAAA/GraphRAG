package com.graphrag.data.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 图数据库操作服务
 */
@Service
public class GraphService {

    private static final Logger logger = LoggerFactory.getLogger(GraphService.class);

    @Autowired
    private Driver driver;

    /**
     * 执行 Cypher 查询
     */
    public List<Map<String, Object>> executeCypher(String cypher, Map<String, Object> parameters) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                var result = tx.run(cypher, parameters);
                return result.list(record -> record.asMap());
            });
        } catch (Exception e) {
            logger.error("执行 Cypher 查询失败: {}", cypher, e);
            throw new RuntimeException("查询执行失败", e);
        }
    }

    /**
     * 执行 Cypher 写操作
     */
    public void executeCypherWrite(String cypher, Map<String, Object> parameters) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(cypher, parameters);
                return null;
            });
        } catch (Exception e) {
            logger.error("执行 Cypher 写操作失败: {}", cypher, e);
            throw new RuntimeException("写操作执行失败", e);
        }
    }

    /**
     * 创建实体之间的关系
     */
    public void createRelationship(String entity1Name, String entity1Type, 
                                 String entity2Name, String entity2Type, 
                                 String relationshipType, String description, Double weight) {
        String cypher = """
            MERGE (e1:Entity {name: $entity1Name, type: $entity1Type})
            MERGE (e2:Entity {name: $entity2Name, type: $entity2Type})
            MERGE (e1)-[r:%s]->(e2)
            SET r.description = $description,
                r.weight = $weight,
                r.created_at = datetime(),
                r.updated_at = datetime()
            """.formatted(relationshipType);

        Map<String, Object> parameters = Map.of(
            "entity1Name", entity1Name,
            "entity1Type", entity1Type,
            "entity2Name", entity2Name,
            "entity2Type", entity2Type,
            "description", description != null ? description : "",
            "weight", weight != null ? weight : 1.0
        );

        executeCypherWrite(cypher, parameters);
        logger.info("创建关系成功: {} -[{}]-> {}", entity1Name, relationshipType, entity2Name);
    }

    /**
     * 创建文档与实体的关系
     */
    public void createDocumentEntityRelationship(Long documentId, String entityName, String entityType) {
        String cypher = """
            MATCH (d:Document) WHERE d.id = $documentId
            MERGE (e:Entity {name: $entityName, type: $entityType})
            MERGE (d)-[r:CONTAINS]->(e)
            SET r.created_at = datetime()
            """;

        Map<String, Object> parameters = Map.of(
            "documentId", documentId,
            "entityName", entityName,
            "entityType", entityType
        );

        executeCypherWrite(cypher, parameters);
        logger.info("创建文档-实体关系成功: Document[{}] -> Entity[{}]", documentId, entityName);
    }

    /**
     * 创建向量索引
     */
    public void createVectorIndex(String indexName, String nodeLabel, String propertyName, Integer dimensions) {
        String cypher = """
            CREATE VECTOR INDEX %s IF NOT EXISTS
            FOR (n:%s) ON (n.%s)
            OPTIONS {
              indexConfig: {
                `vector.dimensions`: %d,
                `vector.similarity_function`: 'cosine'
              }
            }
            """.formatted(indexName, nodeLabel, propertyName, dimensions);

        try {
            executeCypherWrite(cypher, Map.of());
            logger.info("创建向量索引成功: {}", indexName);
        } catch (Exception e) {
            logger.warn("创建向量索引失败，可能已存在: {}", indexName);
        }
    }

    /**
     * 初始化数据库约束和索引
     */
    public void initializeDatabase() {
        logger.info("开始初始化数据库约束和索引...");

        // 创建唯一约束
        String[] constraints = {
            "CREATE CONSTRAINT entity_name_type IF NOT EXISTS FOR (e:Entity) REQUIRE (e.name, e.type) IS UNIQUE",
            "CREATE CONSTRAINT document_title IF NOT EXISTS FOR (d:Document) REQUIRE d.title IS UNIQUE"
        };

        for (String constraint : constraints) {
            try {
                executeCypherWrite(constraint, Map.of());
                logger.info("创建约束成功: {}", constraint);
            } catch (Exception e) {
                logger.warn("创建约束失败，可能已存在: {}", constraint);
            }
        }

        // 创建普通索引
        String[] indexes = {
            "CREATE INDEX entity_name IF NOT EXISTS FOR (e:Entity) ON (e.name)",
            "CREATE INDEX entity_type IF NOT EXISTS FOR (e:Entity) ON (e.type)",
            "CREATE INDEX document_source IF NOT EXISTS FOR (d:Document) ON (d.source)"
        };

        for (String index : indexes) {
            try {
                executeCypherWrite(index, Map.of());
                logger.info("创建索引成功: {}", index);
            } catch (Exception e) {
                logger.warn("创建索引失败，可能已存在: {}", index);
            }
        }

        // 创建向量索引
        createVectorIndex("document_embedding_index", "Document", "embedding", 1536);
        createVectorIndex("entity_embedding_index", "Entity", "embedding", 1536);

        logger.info("数据库初始化完成");
    }

    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStats() {
        String cypher = """
            MATCH (n)
            WITH labels(n) AS nodeLabels
            UNWIND nodeLabels AS label
            RETURN label, count(*) AS count
            ORDER BY count DESC
            """;

        List<Map<String, Object>> results = executeCypher(cypher, Map.of());
        
        String relationshipCypher = """
            MATCH ()-[r]->()
            RETURN type(r) AS relationshipType, count(*) AS count
            ORDER BY count DESC
            """;

        List<Map<String, Object>> relationshipResults = executeCypher(relationshipCypher, Map.of());

        return Map.of(
            "nodes", results,
            "relationships", relationshipResults
        );
    }
}

