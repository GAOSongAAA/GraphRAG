package com.graphrag.data.entity;

import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * 关系实体
 */
@RelationshipProperties
public class RelationshipEntity {

    @Id
    @GeneratedValue                      // internalId
    private Long id;

    @Property("type")
    private String type;

    @Property("description")
    private String description;

    @Property("weight")
    private Double weight;

    @Property("properties")
    private String properties;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    public RelationshipEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.weight = 1.0;
    }

    public RelationshipEntity(String type) {
        this();
        this.type = type;
    }

    public RelationshipEntity(String type, String description) {
        this(type);
        this.description = description;
    }

    public RelationshipEntity(String type, String description, Double weight) {
        this(type, description);
        this.weight = weight;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

