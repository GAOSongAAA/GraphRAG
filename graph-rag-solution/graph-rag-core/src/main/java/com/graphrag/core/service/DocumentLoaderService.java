package com.graphrag.core.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentLoader;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载服務
 */
@Service
public class DocumentLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentLoaderService.class);

    private final DocumentParser textParser = new TextDocumentParser();

    /**
     * 從文件路徑加載文檔
     */
    public Document loadFromFile(Path filePath) {
        try {
            String text = Files.readString(filePath, StandardCharsets.UTF_8);
            Metadata metadata = new Metadata();
            metadata.add("source", filePath.toString());
            return Document.from(text, metadata);
        } catch (Exception e) {
            logger.error("加载文件失败: {}", filePath, e);
            throw new RuntimeException("文件加载失败", e);
        }
    }

    /**
     * 從 URL 加載文檔
     */
    public Document loadFromUrl(String url) {
        try (InputStream is = new URL(url).openStream()) {
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Metadata metadata = new Metadata();
            metadata.add("source", url);
            return Document.from(text, metadata);
        } catch (Exception e) {
            logger.error("加载URL失败: {}", url, e);
            throw new RuntimeException("URL加载失败", e);
        }
    }

    /**
     * 從輸入流加載文檔
     */
    public Document loadFromInputStream(InputStream inputStream, String source) {
        try {
            Document document = textParser.parse(inputStream);
            // 設置文檔來源
            Metadata metadata = new Metadata();
            metadata.add("source", source);
            document = Document.from(document.text(), metadata);
            logger.info("從輸入流加載文檔成功: {}", source);
            return document;
        } catch (Exception e) {
            logger.error("從輸入流加載文檔失敗: {}", source, e);
            throw new RuntimeException("文檔加載失敗", e);
        }
    }

    /**
     * 從文本創建文檔
     */
    public Document createFromText(String text, String source) {
        Metadata metadata = new Metadata();
        metadata.add("source", source);
        Document document = Document.from(text, metadata);
        logger.info("從文本創建文檔成功，來源: {}, 長度: {}", source, text.length());
        return document;
    }

    /**
     * 批量加載目錄下的所有文檔
     */
    public List<Document> loadFromDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                throw new IllegalArgumentException("目錄不存在或不是有效目錄: " + directoryPath);
            }
            List<Document> documents = new ArrayList<>();
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            Document document = loadFromFile(file.toPath());
                            documents.add(document);
                        } catch (Exception e) {
                            logger.warn("加載文件失敗，跳過: {}", file.getPath(), e);
                        }
                    }
                }
            }
            logger.info("從目錄批量加載文檔成功: {}, 文檔數量: {}", directoryPath, documents.size());
            return documents;
        } catch (Exception e) {
            logger.error("從目錄批量加載文檔失敗: {}", directoryPath, e);
            throw new RuntimeException("批量文檔加載失敗", e);
        }
    }

    /**
     * 批量加載指定文件列表
     */
    public List<Document> loadFromFiles(List<String> filePaths) {
        try {
            List<Document> documents = new ArrayList<>();
            for (String filePath : filePaths) {
                try {
                    Document document = loadFromFile(Path.of(filePath));
                    documents.add(document);
                } catch (Exception e) {
                    logger.warn("加載文件失敗，跳過: {}", filePath, e);
                }
            }
            logger.info("批量加載文檔成功，文檔數量: {}", documents.size());
            return documents;
        } catch (Exception e) {
            logger.error("批量加載文檔失敗，文件數量: {}", filePaths.size(), e);
            throw new RuntimeException("批量文檔加載失敗", e);
        }
    }

    /**
     * 驗證文檔內容
     */
    public boolean validateDocument(Document document) {
        if (document == null) {
            logger.warn("文檔為空");
            return false;
        }

        if (document.text() == null || document.text().trim().isEmpty()) {
            logger.warn("文檔內容為空");
            return false;
        }

        if (document.text().length() < 10) {
            logger.warn("文檔內容過短: {}", document.text().length());
            return false;
        }

        return true;
    }

    /**
     * 獲取文檔統計信息
     */
    public DocumentStats getDocumentStats(Document document) {
        if (document == null || document.text() == null) {
            return new DocumentStats(0, 0, 0, 0);
        }

        String text = document.text();
        int characterCount = text.length();
        int wordCount = text.split("\\s+").length;
        int lineCount = text.split("\n").length;
        int paragraphCount = text.split("\n\n").length;

        return new DocumentStats(characterCount, wordCount, lineCount, paragraphCount);
    }

    /**
     * 文檔統計信息類
     */
    public static class DocumentStats {
        private final int characterCount;
        private final int wordCount;
        private final int lineCount;
        private final int paragraphCount;

        public DocumentStats(int characterCount, int wordCount, int lineCount, int paragraphCount) {
            this.characterCount = characterCount;
            this.wordCount = wordCount;
            this.lineCount = lineCount;
            this.paragraphCount = paragraphCount;
        }

        // Getters
        public int getCharacterCount() { return characterCount; }
        public int getWordCount() { return wordCount; }
        public int getLineCount() { return lineCount; }
        public int getParagraphCount() { return paragraphCount; }

        @Override
        public String toString() {
            return String.format("DocumentStats{characters=%d, words=%d, lines=%d, paragraphs=%d}",
                    characterCount, wordCount, lineCount, paragraphCount);
        }
    }
}
