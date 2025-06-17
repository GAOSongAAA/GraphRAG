package com.graphrag.data.repository;

import com.graphrag.data.entity.EntityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Entity nodes, fully migrated to elementId() / String ID.
 */
@Repository
public interface EntityRepository extends Neo4jRepository<EntityNode, Long> {

    /** Find entity by exact name */
    Optional<EntityNode> findByName(String name);

    /** Find entities by type */
    List<EntityNode> findByType(String type);

    /** Find entity by name + type */
    @Query("""
           MATCH (e:Entity {name:$name, type:$type})
           RETURN e LIMIT 1
           """)
    Optional<EntityNode> findByNameAndType(@Param("name") String name,
                                           @Param("type") String type);

    /** Fetch one‑hop neighbours */
    @Query("""
           MATCH (e1:Entity {name:$entityName})-[r]-(e2:Entity)
           RETURN e2
           """)
    List<EntityNode> findRelatedEntities(@Param("entityName") String entityName);

    /** Fetch neighbours via a specific relationship type */
    @Query("""
           MATCH (e1:Entity {name:$entityName})-[r]-(e2:Entity)
           WHERE r.type = $relationType
           RETURN e2
           """)
    List<EntityNode> findRelatedEntitiesByType(@Param("entityName") String entityName,
                                               @Param("relationType") String relationType);

    /** Shortest path between two entities */
    @Query("""
           MATCH path = shortestPath((e1:Entity {name:$entity1})-[*]-(e2:Entity {name:$entity2}))
           RETURN path
           """)
    List<Object> findShortestPath(@Param("entity1") String entity1,
                                  @Param("entity2") String entity2);

    /** Cosine‑similar entity retrieval */
    @Query("""
           MATCH (e:Entity)
           WHERE e.embedding IS NOT NULL
           WITH e, gds.similarity.cosine(e.embedding, $queryEmbedding) AS similarity
           WHERE similarity > $threshold
           RETURN e
           ORDER BY similarity DESC
           LIMIT $limit
           """)
    List<EntityNode> findSimilarEntities(@Param("queryEmbedding") List<Double> queryEmbedding,
                                         @Param("threshold") double threshold,
                                         @Param("limit") int limit);

    /** Degree‑centrality based importance */
    @Query("""
           MATCH (e:Entity)
           WITH e, size((e)--()) AS degree
           RETURN e
           ORDER BY degree DESC
           LIMIT $limit
           """)
    List<EntityNode> findMostConnectedEntities(@Param("limit") int limit);
}