-- Neo4j 数据库初始化脚本

-- 创建唯一约束
CREATE CONSTRAINT entity_name_type IF NOT EXISTS FOR (e:Entity) REQUIRE (e.name, e.type) IS UNIQUE;
CREATE CONSTRAINT document_title IF NOT EXISTS FOR (d:Document) REQUIRE d.title IS UNIQUE;

-- 创建索引
CREATE INDEX entity_name IF NOT EXISTS FOR (e:Entity) ON (e.name);
CREATE INDEX entity_type IF NOT EXISTS FOR (e:Entity) ON (e.type);
CREATE INDEX document_source IF NOT EXISTS FOR (d:Document) ON (d.source);
CREATE INDEX document_created_at IF NOT EXISTS FOR (d:Document) ON (d.created_at);
CREATE INDEX entity_created_at IF NOT EXISTS FOR (e:Entity) ON (e.created_at);

-- 创建向量索引（Neo4j 5.x 支持）
CREATE VECTOR INDEX document_embedding_index IF NOT EXISTS
FOR (d:Document) ON (d.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  }
};

CREATE VECTOR INDEX entity_embedding_index IF NOT EXISTS
FOR (e:Entity) ON (e.embedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  }
};

-- 创建全文索引
CREATE FULLTEXT INDEX document_content_index IF NOT EXISTS
FOR (d:Document) ON EACH [d.title, d.content];

CREATE FULLTEXT INDEX entity_description_index IF NOT EXISTS
FOR (e:Entity) ON EACH [e.name, e.description];

-- 示例数据插入（可选）
-- 创建示例文档
MERGE (d1:Document {
  title: "人工智能概述",
  content: "人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。",
  source: "example",
  created_at: datetime(),
  updated_at: datetime()
});

-- 创建示例实体
MERGE (e1:Entity {
  name: "人工智能",
  type: "概念",
  description: "模拟人类智能的计算机系统",
  created_at: datetime(),
  updated_at: datetime()
});

MERGE (e2:Entity {
  name: "机器学习",
  type: "技术",
  description: "使计算机能够从数据中学习的技术",
  created_at: datetime(),
  updated_at: datetime()
});

MERGE (e3:Entity {
  name: "深度学习",
  type: "技术",
  description: "基于神经网络的机器学习方法",
  created_at: datetime(),
  updated_at: datetime()
});

-- 创建示例关系
MERGE (e1)-[r1:INCLUDES]->(e2)
SET r1.description = "人工智能包含机器学习",
    r1.weight = 0.9,
    r1.created_at = datetime();

MERGE (e2)-[r2:INCLUDES]->(e3)
SET r2.description = "机器学习包含深度学习",
    r2.weight = 0.8,
    r2.created_at = datetime();

-- 创建文档与实体的关系
MATCH (d:Document {title: "人工智能概述"})
MATCH (e:Entity {name: "人工智能"})
MERGE (d)-[r:CONTAINS]->(e)
SET r.created_at = datetime();

