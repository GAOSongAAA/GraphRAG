package com.graphrag.core.algorithm;

import com.graphrag.core.algorithm.ContextFusionAlgorithm.FusedContext;
import com.graphrag.core.algorithm.QueryUnderstandingAlgorithm.QueryAnalysis;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 答案生成策略算法
 */
@Component
public class AnswerGenerationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationAlgorithm.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    // 不同类型查询的提示模板
    private static final PromptTemplate FACTUAL_TEMPLATE = PromptTemplate.from("""
            基于以下上下文信息，回答用户的事实性问题。请提供准确、简洁的答案。
            
            上下文信息：
            {{context}}
            
            用户问题：{{question}}
            
            请直接回答问题，如果上下文信息不足，请明确说明。
            
            回答：
            """);

    private static final PromptTemplate CONCEPTUAL_TEMPLATE = PromptTemplate.from("""
            基于以下上下文信息，详细解释相关概念。请提供全面、易懂的解释。
            
            上下文信息：
            {{context}}
            
            用户问题：{{question}}
            
            请提供详细的概念解释，包括定义、特点、应用等方面。
            
            回答：
            """);

    private static final PromptTemplate COMPARATIVE_TEMPLATE = PromptTemplate.from("""
            基于以下上下文信息，进行比较分析。请从多个维度进行对比。
            
            上下文信息：
            {{context}}
            
            用户问题：{{question}}
            
            请提供结构化的比较分析，包括相同点、不同点、优缺点等。
            
            回答：
            """);

    private static final PromptTemplate REASONING_TEMPLATE = PromptTemplate.from("""
            基于以下上下文信息，进行推理分析。请提供逻辑清晰的推理过程。
            
            上下文信息：
            {{context}}
            
            用户问题：{{question}}
            
            请提供推理过程和结论，确保逻辑严密。
            
            回答：
            """);

    private static final PromptTemplate LIST_TEMPLATE = PromptTemplate.from("""
            基于以下上下文信息，提供列表形式的回答。
            
            上下文信息：
            {{context}}
            
            用户问题：{{question}}
            
            请以清晰的列表形式组织答案。
            
            回答：
            """);

    /**
     * 生成答案
     */
    public String generateAnswer(String question, FusedContext context, QueryAnalysis queryAnalysis) {
        logger.debug("开始生成答案，查询类型: {}", queryAnalysis.getQueryType());

        try {
            // 根据查询类型选择合适的模板
            PromptTemplate template = selectTemplate(queryAnalysis.getQueryType());
            
            // 构建提示
            Prompt prompt = template.apply(Map.of(
                    "question", question,
                    "context", context.getContextText()
            ));

            // 生成答案
            String answer = chatLanguageModel.generate(prompt.text());
            
            // 后处理答案
            answer = postProcessAnswer(answer, queryAnalysis);
            
            logger.info("答案生成完成，长度: {}", answer.length());
            return answer;

        } catch (Exception e) {
            logger.error("答案生成失败", e);
            return generateFallbackAnswer(question, context);
        }
    }

    /**
     * 选择提示模板
     */
    private PromptTemplate selectTemplate(String queryType) {
        switch (queryType) {
            case "事实查询":
                return FACTUAL_TEMPLATE;
            case "概念解释":
                return CONCEPTUAL_TEMPLATE;
            case "比较分析":
                return COMPARATIVE_TEMPLATE;
            case "推理问答":
                return REASONING_TEMPLATE;
            case "列表查询":
                return LIST_TEMPLATE;
            default:
                return CONCEPTUAL_TEMPLATE; // 默认使用概念解释模板
        }
    }

    /**
     * 后处理答案
     */
    private String postProcessAnswer(String answer, QueryAnalysis queryAnalysis) {
        // 1. 清理格式
        answer = answer.trim();
        
        // 2. 根据期望答案类型调整格式
        switch (queryAnalysis.getExpectedAnswerType()) {
            case "简短回答":
                answer = extractShortAnswer(answer);
                break;
            case "列表":
                answer = formatAsList(answer);
                break;
            case "比较表格":
                answer = formatAsComparison(answer);
                break;
        }

        // 3. 添加置信度信息（如果相关性较低）
        if (queryAnalysis.getQueryVector() != null) {
            // 这里可以添加置信度评估逻辑
        }

        return answer;
    }

    /**
     * 提取简短答案
     */
    private String extractShortAnswer(String answer) {
        // 提取第一句话作为简短答案
        String[] sentences = answer.split("[。！？]");
        if (sentences.length > 0) {
            return sentences[0].trim() + "。";
        }
        return answer.length() > 100 ? answer.substring(0, 100) + "..." : answer;
    }

    /**
     * 格式化为列表
     */
    private String formatAsList(String answer) {
        if (answer.contains("1.") || answer.contains("•") || answer.contains("-")) {
            return answer; // 已经是列表格式
        }
        
        // 尝试将段落转换为列表
        String[] paragraphs = answer.split("\n\n");
        if (paragraphs.length > 1) {
            StringBuilder listAnswer = new StringBuilder();
            for (int i = 0; i < paragraphs.length; i++) {
                listAnswer.append((i + 1)).append(". ").append(paragraphs[i].trim()).append("\n");
            }
            return listAnswer.toString();
        }
        
        return answer;
    }

    /**
     * 格式化为比较
     */
    private String formatAsComparison(String answer) {
        // 简单的比较格式化
        if (answer.contains("相同点") || answer.contains("不同点") || answer.contains("对比")) {
            return answer; // 已经是比较格式
        }
        
        return "比较分析：\n" + answer;
    }

    /**
     * 生成备用答案
     */
    private String generateFallbackAnswer(String question, FusedContext context) {
        if (context.getContextText().trim().isEmpty()) {
            return "抱歉，我没有找到足够的相关信息来回答您的问题。请尝试重新表述您的问题或提供更多上下文。";
        }
        
        return "基于可用信息，我尝试回答您的问题：\n\n" + 
               context.getContextText().substring(0, Math.min(500, context.getContextText().length())) + 
               "\n\n请注意，这个回答可能不够完整，建议您查阅更多资料。";
    }

    /**
     * 多轮对话答案生成
     */
    public String generateConversationalAnswer(String question, FusedContext context, 
                                             QueryAnalysis queryAnalysis, String conversationHistory) {
        logger.debug("生成多轮对话答案");

        PromptTemplate conversationalTemplate = PromptTemplate.from("""
                基于以下对话历史和上下文信息，回答用户的问题。请保持对话的连贯性。
                
                对话历史：
                {{history}}
                
                上下文信息：
                {{context}}
                
                当前问题：{{question}}
                
                请提供连贯、相关的回答。
                
                回答：
                """);

        try {
            Prompt prompt = conversationalTemplate.apply(Map.of(
                    "question", question,
                    "context", context.getContextText(),
                    "history", conversationHistory != null ? conversationHistory : "无"
            ));

            return chatLanguageModel.generate(prompt.text());

        } catch (Exception e) {
            logger.error("多轮对话答案生成失败", e);
            return generateAnswer(question, context, queryAnalysis);
        }
    }

    /**
     * 生成解释性答案
     */
    public String generateExplanatoryAnswer(String question, FusedContext context, 
                                          QueryAnalysis queryAnalysis) {
        logger.debug("生成解释性答案");

        PromptTemplate explanatoryTemplate = PromptTemplate.from("""
                请详细解释以下问题，提供全面的背景信息和深入分析。
                
                上下文信息：
                {{context}}
                
                问题：{{question}}
                
                请提供：
                1. 基本概念解释
                2. 相关背景信息
                3. 详细分析
                4. 实际应用或例子
                
                回答：
                """);

        try {
            Prompt prompt = explanatoryTemplate.apply(Map.of(
                    "question", question,
                    "context", context.getContextText()
            ));

            return chatLanguageModel.generate(prompt.text());

        } catch (Exception e) {
            logger.error("解释性答案生成失败", e);
            return generateAnswer(question, context, queryAnalysis);
        }
    }

    /**
     * 生成结构化答案
     */
    public StructuredAnswer generateStructuredAnswer(String question, FusedContext context, 
                                                   QueryAnalysis queryAnalysis) {
        logger.debug("生成结构化答案");

        try {
            String mainAnswer = generateAnswer(question, context, queryAnalysis);
            
            StructuredAnswer structuredAnswer = new StructuredAnswer();
            structuredAnswer.setMainAnswer(mainAnswer);
            structuredAnswer.setConfidence(calculateConfidence(context, queryAnalysis));
            structuredAnswer.setSourceCount(context.getSegments().size());
            structuredAnswer.setAnswerType(queryAnalysis.getExpectedAnswerType());
            structuredAnswer.setKeyPoints(extractKeyPoints(mainAnswer));
            
            return structuredAnswer;

        } catch (Exception e) {
            logger.error("结构化答案生成失败", e);
            return createFallbackStructuredAnswer(question);
        }
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(FusedContext context, QueryAnalysis queryAnalysis) {
        // 基于上下文相关性和查询复杂度计算置信度
        double contextRelevance = context.getOverallRelevance();
        double complexityFactor = getComplexityFactor(queryAnalysis.getComplexity());
        
        return Math.min(1.0, contextRelevance * complexityFactor);
    }

    /**
     * 获取复杂度因子
     */
    private double getComplexityFactor(String complexity) {
        switch (complexity) {
            case "简单": return 1.0;
            case "中等": return 0.8;
            case "复杂": return 0.6;
            default: return 0.8;
        }
    }

    /**
     * 提取关键点
     */
    private String[] extractKeyPoints(String answer) {
        // 简单的关键点提取
        String[] sentences = answer.split("[。！？]");
        return java.util.Arrays.stream(sentences)
                .filter(s -> s.trim().length() > 10)
                .limit(3)
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * 创建备用结构化答案
     */
    private StructuredAnswer createFallbackStructuredAnswer(String question) {
        StructuredAnswer answer = new StructuredAnswer();
        answer.setMainAnswer("抱歉，无法生成满意的答案。");
        answer.setConfidence(0.1);
        answer.setSourceCount(0);
        answer.setAnswerType("错误");
        answer.setKeyPoints(new String[]{"信息不足"});
        return answer;
    }

    /**
     * 结构化答案类
     */
    public static class StructuredAnswer {
        private String mainAnswer;
        private double confidence;
        private int sourceCount;
        private String answerType;
        private String[] keyPoints;

        // Getters and Setters
        public String getMainAnswer() { return mainAnswer; }
        public void setMainAnswer(String mainAnswer) { this.mainAnswer = mainAnswer; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getSourceCount() { return sourceCount; }
        public void setSourceCount(int sourceCount) { this.sourceCount = sourceCount; }

        public String getAnswerType() { return answerType; }
        public void setAnswerType(String answerType) { this.answerType = answerType; }

        public String[] getKeyPoints() { return keyPoints; }
        public void setKeyPoints(String[] keyPoints) { this.keyPoints = keyPoints; }
    }
}

