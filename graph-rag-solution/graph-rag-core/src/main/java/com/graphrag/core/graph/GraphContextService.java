package com.graphrag.core.graph;

import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.service.GraphService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Retrieves sub‑graphs around the supplied entities for additional context.
 */
@Service
public class GraphContextService {

    private final GraphService graphService;

    public GraphContextService(GraphService graphService) {
        this.graphService = graphService;
    }

    public List<Map<String, Object>> retrieve(List<EntityNode> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        String cypher = """
                MATCH (e1:Entity)-[r]-(e2:Entity)
                WHERE e1.name IN $entityNames
                RETURN e1.name AS entity1, type(r) AS relationship, e2.name AS entity2, r.description AS description
                LIMIT 50
                """;
        List<String> names = entities.stream().map(EntityNode::getName).collect(Collectors.toList());
        return graphService.executeCypher(cypher, Map.of("entityNames", names));
    }
}