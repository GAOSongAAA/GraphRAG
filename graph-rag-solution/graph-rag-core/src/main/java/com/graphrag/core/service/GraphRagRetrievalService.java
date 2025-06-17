package com.graphrag.core.service;

import com.graphrag.core.algorithm.ContextFusionAlgorithm;
import com.graphrag.core.algorithm.ContextFusionAlgorithm.FusedContext;
import com.graphrag.core.context.ContextBuilder;
import com.graphrag.core.graph.GraphContextService;
import com.graphrag.core.prompt.RAGPromptTemplates;
import com.graphrag.core.utils.MergeUtil;
import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * High‑level orchestrator that wires together vector retrieval, graph traversal
 * and LLM generation.
 */
@Service
public class GraphRagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagRetrievalService.class);

    private final ChatLanguageModel chatModel;
    private final EmbeddingService embedSvc;
    private final DocumentService docSvc;
    private final EntityService entitySvc;
    private final GraphContextService graphCtxSvc;
    private final ContextFusionAlgorithm fusionAlgorithm;
    private final ContextBuilder ctxBuilder = new ContextBuilder();

    public GraphRagRetrievalService(ChatLanguageModel chatModel,
            EmbeddingService embedSvc,
            DocumentService docSvc,
            EntityService entitySvc,
            GraphContextService graphCtxSvc,
            ContextFusionAlgorithm fusionAlgorithm) {
        this.chatModel = chatModel;
        this.embedSvc = embedSvc;
        this.docSvc = docSvc;
        this.entitySvc = entitySvc;
        this.graphCtxSvc = graphCtxSvc;
        this.fusionAlgorithm = fusionAlgorithm;
    }

    /**
     * Vector‑only retrieval path.
     */
    public GraphRagResponse retrieve(GraphRagRequest req) {
        log.info("Graph RAG retrieve – question: {}", req.getQuestion());
        try {
            List<Double> qEmbed = embedSvc.embedText(req.getQuestion());
            List<DocumentNode> docs = docSvc.findSimilarDocuments(qEmbed, 0.7, 5);
            List<EntityNode> ents = entitySvc.findSimilarEntities(qEmbed, 0.7, 10);
            List<Map<String, Object>> relations = graphCtxSvc.retrieve(ents);

            FusedContext fused = fusionAlgorithm.fuseMultiSourceContext(docs, ents, relations, req.getQuestion());
            String answer = generateAnswer(req.getQuestion(), fused.getContextText());
            return buildResponse(req.getQuestion(), answer, docs, ents);
        } catch (Exception ex) {
            log.error("Graph RAG retrieve failed", ex);
            throw new RuntimeException("Retrieval failed", ex);
        }
    }

    /**
     * Hybrid retrieval combining vector, keyword and graph traversal.
     */
    public GraphRagResponse hybridRetrieve(GraphRagRequest req) {
        log.info("Hybrid RAG retrieve – question: {}", req.getQuestion());
        try {
            List<Double> qEmbed = embedSvc.embedText(req.getQuestion());
            List<DocumentNode> vecDocs = docSvc.findSimilarDocuments(qEmbed, 0.6, 3);
            List<EntityNode> vecEnts = entitySvc.findSimilarEntities(qEmbed, 0.6, 5);
            List<DocumentNode> kwDocs = docSvc.searchByContent(req.getQuestion());
            List<EntityNode> graphEnts = findEntitiesByGraphTraversal(req.getQuestion());

            List<DocumentNode> allDocs = MergeUtil.merge(10, vecDocs, kwDocs);
            List<EntityNode> allEnts = MergeUtil.merge(15, vecEnts, graphEnts);
            List<Map<String, Object>> relations = graphCtxSvc.retrieve(allEnts);

            FusedContext fused = fusionAlgorithm.fuseMultiSourceContext(allDocs, allEnts, relations, req.getQuestion());
            String answer = generateAnswer(req.getQuestion(), fused.getContextText());
            return buildResponse(req.getQuestion(), answer, allDocs, allEnts);
        } catch (Exception ex) {
            log.error("Hybrid RAG retrieve failed", ex);
            throw new RuntimeException("Hybrid retrieval failed", ex);
        }
    }

    // -------------------------------------------
    // Internal helpers
    // -------------------------------------------

    private String generateAnswer(String question, String context) {
        Prompt prompt = RAGPromptTemplates.RAG_TEMPLATE.apply(Map.of("question", question, "context", context));
        return chatModel.generate(prompt.text());
    }

    private GraphRagResponse buildResponse(String q, String a, List<DocumentNode> docs, List<EntityNode> ents) {
        GraphRagResponse res = new GraphRagResponse();
        res.setQuestion(q);
        res.setAnswer(a);
        res.setRelevantDocuments(docs.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("title", d.getTitle());
            m.put("source", d.getSource());
            return m;
        }).collect(Collectors.toList()));
        res.setRelevantEntities(ents.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("type", e.getType());
            return m;
        }).collect(Collectors.toList()));
        return res;
    }

    private List<EntityNode> findEntitiesByGraphTraversal(String question) {
        String[] kws = question.toLowerCase().split("\\s+");
        return entitySvc.findAll().stream()
                .filter(e -> {
                    String txt = (e.getName() + " " + e.getDescription()).toLowerCase();
                    for (String kw : kws) {
                        if (txt.contains(kw))
                            return true;
                    }
                    return false;
                })
                .limit(5)
                .collect(Collectors.toList());
    }
}