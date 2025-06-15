package com.graphrag.data.entity;

import org.springframework.data.neo4j.core.schema.*;

/**
 * 实体关系
 */
@RelationshipProperties
public class EntityRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private EntityNode target;

    @Property("type")
    private String type;

    @Property("description")
    private String description;

    @Property("weight")
    private Double weight;

    public EntityRelationship() {
        this.weight = 1.0;
    }

    public EntityRelationship(EntityNode target, String type) {
        this();
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

    // Getters and Setters
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

