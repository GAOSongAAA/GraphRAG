package com.graphrag.core.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Embedding Service
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Generate text embedding vector
     */
    public List<Double> embedText(String text) {
        try {
            Response<Embedding> response = embeddingModel.embed(text);
            List<Float> floatVector = response.content().vectorAsList();
            List<Double> vector = floatVector.stream().map(Float::doubleValue).collect(Collectors.toList());
            logger.debug("Generated embedding vector successfully, text length: {}, vector dimension: {}", text.length(), vector.size());
            return vector;
        } catch (Exception e) {
            logger.error("Failed to generate embedding vector, text: {}", text.substring(0, Math.min(100, text.length())), e);
            throw new RuntimeException("Failed to generate embedding vector", e);
        }
    }

    /**
     * Generate text segment embedding vector
     */
    public List<Double> embedTextSegment(TextSegment segment) {
        return embedText(segment.text());
    }

    /**
     * Batch generate embedding vectors
     */
    public List<List<Double>> embedTexts(List<String> texts) {
        try {
            List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            List<List<Double>> vectors = response.content().stream()
                .map(embedding -> embedding.vectorAsList().stream()
                    .map(Float::doubleValue)
                    .collect(Collectors.toList()))
                .collect(Collectors.toList());
            return vectors;
        } catch (Exception e) {
            logger.error("Failed to batch generate embedding vectors, text count: {}", texts.size(), e);
            throw new RuntimeException("Failed to batch generate embedding vectors", e);
        }
    }

    /**
     * Batch generate text segment embedding vectors
     */
    public List<List<Double>> embedTextSegments(List<TextSegment> segments) {
        try {
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            List<List<Double>> vectors = response.content().stream()
                .map(embedding -> embedding.vectorAsList().stream().map(Float::doubleValue).collect(Collectors.toList()))
                .collect(Collectors.toList());
            return vectors;
        } catch (Exception e) {
            logger.error("Failed to batch generate text segment embedding vectors, segment count: {}", segments.size(), e);
            throw new RuntimeException("Failed to batch generate text segment embedding vectors", e);
        }
    }

    /**
     * Asynchronously generate embedding vector
     */
    public CompletableFuture<List<Double>> embedTextAsync(String text) {
        return CompletableFuture.supplyAsync(() -> embedText(text), executorService);
    }

    /**
     * Asynchronously batch generate embedding vectors
     */
    public CompletableFuture<List<List<Double>>> embedTextsAsync(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> embedTexts(texts), executorService);
    }

    /**
     * Calculate cosine similarity
     */
    public double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vector dimensions do not match");
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
     * Find the most similar vector
     */
    public int findMostSimilar(List<Double> queryVector, List<List<Double>> candidateVectors) {
        double maxSimilarity = -1.0;
        int mostSimilarIndex = -1;

        for (int i = 0; i < candidateVectors.size(); i++) {
            double similarity = cosineSimilarity(queryVector, candidateVectors.get(i));
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarIndex = i;
            }
        }

        return mostSimilarIndex;
    }

    /**
     * Find top K similar vectors
     */
    public List<Integer> findTopKSimilar(List<Double> queryVector, List<List<Double>> candidateVectors, int k) {
        List<SimilarityResult> results = new ArrayList<>();

        for (int i = 0; i < candidateVectors.size(); i++) {
            double similarity = cosineSimilarity(queryVector, candidateVectors.get(i));
            results.add(new SimilarityResult(i, similarity));
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(k)
                .map(result -> result.index)
                .collect(Collectors.toList());
    }

    /**
     * Internal class for similarity results
     */
    private static class SimilarityResult {
        final int index;
        final double similarity;

        SimilarityResult(int index, double similarity) {
            this.index = index;
            this.similarity = similarity;
        }
    }

    /**
     * Shutdown thread pool
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
