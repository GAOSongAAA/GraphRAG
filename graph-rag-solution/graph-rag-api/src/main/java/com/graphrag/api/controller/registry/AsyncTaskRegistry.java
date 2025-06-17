package com.graphrag.api.controller.registry;

import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.graphrag.core.model.GraphRagResponse;

@Component
public class AsyncTaskRegistry {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskRegistry.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final ConcurrentMap<String, CompletableFuture<GraphRagResponse>> tasks = new ConcurrentHashMap<>();

    public String submit(Supplier<GraphRagResponse> supplier) {
        String id = UUID.randomUUID().toString();
        CompletableFuture<GraphRagResponse> future =
                CompletableFuture.supplyAsync(supplier, executor)
                                 .whenComplete((r, ex) -> {
                                     if (ex != null) log.error("Task {} failed", id, ex);
                                 });
        tasks.put(id, future);
        return id;
    }

    public Optional<GraphRagResponse> get(String id) {
        CompletableFuture<GraphRagResponse> f = tasks.get(id);
        return f == null || !f.isDone() ? Optional.empty() : Optional.of(f.join());
    }

    public boolean isRunning(String id) {
        CompletableFuture<GraphRagResponse> f = tasks.get(id);
        return f != null && !f.isDone();
    }
}
