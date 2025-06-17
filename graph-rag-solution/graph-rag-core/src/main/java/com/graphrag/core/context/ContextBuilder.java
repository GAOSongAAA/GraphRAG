package com.graphrag.core.context;

import java.util.List;
import java.util.Map;

import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;

public class ContextBuilder {
    public String build(List<DocumentNode> documents, List<EntityNode> entities, List<Map<String, Object>> relations) {
        StringBuilder ctx = new StringBuilder();

        // Documents
        if (!documents.isEmpty()) {
            ctx.append("Related Documents:\n");
            documents.forEach(
                    doc -> ctx.append("- ").append(doc.getTitle()).append(": ").append(doc.getContent()).append("\n"));
            ctx.append("\n");
        }

        // Entities
        if (!entities.isEmpty()) {
            ctx.append("Related Entities:\n");
            entities.forEach(ent -> {
                ctx.append("- ")
                        .append(ent.getName())
                        .append(" (")
                        .append(ent.getType())
                        .append(")");
                if (ent.getDescription() != null) {
                    ctx.append(": ")
                            .append(ent.getDescription());
                }
                ctx.append("\n");
            });
            ctx.append("\n");
        }

        // Relations
        if (!relations.isEmpty()) {
            ctx.append("Entity Relationships:\n");
            relations.forEach(rel -> {
                ctx.append("- ")
                        .append(rel.get("entity1"))
                        .append(" ")
                        .append(rel.get("relationship"))
                        .append(" ")
                        .append(rel.get("entity2"));
                if (rel.get("description") != null) {
                    ctx.append(" (")
                            .append(rel.get("description"))
                            .append(")");
                }
                ctx.append("\n");
            });
        }

        return ctx.toString();
    }

    private String trim(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}
