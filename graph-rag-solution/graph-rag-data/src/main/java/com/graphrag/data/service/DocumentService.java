package com.graphrag.data.service;

import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档服务
 */
@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * 保存文档
     */
    public DocumentNode saveDocument(DocumentNode document) {
        document.setUpdatedAt(LocalDateTime.now());
        DocumentNode saved = documentRepository.save(document);
        logger.info("保存文档成功，ID: {}, 标题: {}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * 根据ID查找文档
     */
    public Optional<DocumentNode> findById(Long id) {
        return documentRepository.findById(id);
    }

    /**
     * 根据标题查找文档
     */
    public Optional<DocumentNode> findByTitle(String title) {
        return documentRepository.findByTitle(title);
    }

    /**
     * 根据来源查找文档
     */
    public List<DocumentNode> findBySource(String source) {
        return documentRepository.findBySource(source);
    }

    /**
     * 根据内容关键词搜索文档
     */
    public List<DocumentNode> searchByContent(String keyword) {
        return documentRepository.findByContentContaining(keyword);
    }

    /**
     * 向量相似性搜索
     */
    public List<DocumentNode> findSimilarDocuments(List<Double> queryEmbedding, Double threshold, Integer limit) {
        if (threshold == null) threshold = 0.7;
        if (limit == null) limit = 10;
        return documentRepository.findSimilarDocuments(queryEmbedding, threshold, limit);
    }

    /**
     * 更新文档嵌入向量
     */
    public DocumentNode updateEmbedding(Long documentId, List<Double> embedding) {
        Optional<DocumentNode> optionalDoc = documentRepository.findById(documentId);
        if (optionalDoc.isPresent()) {
            DocumentNode document = optionalDoc.get();
            document.setEmbedding(embedding);
            document.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(document);
        }
        throw new RuntimeException("文档不存在，ID: " + documentId);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
        logger.info("删除文档成功，ID: {}", id);
    }

    /**
     * 获取所有文档
     */
    public List<DocumentNode> findAll() {
        return documentRepository.findAll();
    }

    /**
     * 获取所有有嵌入向量的文档
     */
    public List<DocumentNode> findAllWithEmbedding() {
        return documentRepository.findAllWithEmbedding();
    }

    /**
     * 批量保存文档
     */
    public List<DocumentNode> saveAll(List<DocumentNode> documents) {
        documents.forEach(doc -> doc.setUpdatedAt(LocalDateTime.now()));
        List<DocumentNode> saved = documentRepository.saveAll(documents);
        logger.info("批量保存文档成功，数量: {}", saved.size());
        return saved;
    }
}

