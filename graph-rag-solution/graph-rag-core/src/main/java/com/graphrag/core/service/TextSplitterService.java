package com.graphrag.core.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本分割服务
 */
@Service
public class TextSplitterService {

    private static final Logger logger = LoggerFactory.getLogger(TextSplitterService.class);

    private final DocumentSplitter documentSplitter;

    public TextSplitterService() {
        // 配置文档分割器
        this.documentSplitter = DocumentSplitters.recursive(
                1000,  // 最大块大小
                200    // 重叠大小
        );
    }

    /**
     * 分割文档
     */
    public List<TextSegment> splitDocument(Document document) {
        logger.debug("开始分割文档，原始长度: {}", document.text().length());
        
        List<TextSegment> segments = documentSplitter.split(document);
        
        logger.info("文档分割完成，生成 {} 个片段", segments.size());
        return segments;
    }

    /**
     * 分割文本
     */
    public List<TextSegment> splitText(String text) {
        Document document = Document.from(text);
        return splitDocument(document);
    }

    /**
     * 分割文本（带元数据）
     */
    public List<TextSegment> splitText(String text, String source) {
        Document document = Document.from(text);
        document.metadata().add("source", source);
        return splitDocument(document);
    }

    /**
     * 自定义分割参数
     */
    public List<TextSegment> splitTextCustom(String text, int maxChunkSize, int overlap) {
        DocumentSplitter customSplitter = DocumentSplitters.recursive(
                maxChunkSize,
                overlap
        );
        Document document = Document.from(text);
        return customSplitter.split(document);
    }

    /**
     * 按句子分割
     */
    public List<TextSegment> splitBySentence(String text, int maxChunkSize) {
        DocumentSplitter sentenceSplitter = DocumentSplitters.recursive(
                maxChunkSize,
                50  // 较小的重叠
        );
        Document document = Document.from(text);
        return sentenceSplitter.split(document);
    }

    /**
     * 按段落分割
     */
    public List<TextSegment> splitByParagraph(String text) {
        DocumentSplitter paragraphSplitter = DocumentSplitters.recursive(
                2000,  // 较大的块大小
                100
        );
        Document document = Document.from(text);
        return paragraphSplitter.split(document);
    }
}

