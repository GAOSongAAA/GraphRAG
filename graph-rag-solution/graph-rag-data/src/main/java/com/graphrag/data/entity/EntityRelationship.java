package com.graphrag.data.entity;

import org.springframework.data.neo4j.core.schema.*;

/**
 * Relationship between two Entity nodes.
 */
@RelationshipProperties
public class EntityRelationship {

    @Id
    @GeneratedValue                      // internalId
    private Long id;

    @TargetNode
    private EntityNode target;

    private String type;
    private String description;
    private Double weight = 1.0;

    public EntityRelationship() {}

    public EntityRelationship(EntityNode target, String type) {
        this.target = target;
        this.type = type;
    }

    public EntityRelationship(EntityNode target, String type, String description) {
        this(target, type);
        this.description = description;
    }

    public EntityRelationship(EntityNode target, String type, String description, Double weight) {
        this(target, type, description);
        this.weight = weight;
    }

    // Getters / Setters ---------------------------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EntityNode getTarget() { return target; }
    public void setTarget(EntityNode target) { this.target = target; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
