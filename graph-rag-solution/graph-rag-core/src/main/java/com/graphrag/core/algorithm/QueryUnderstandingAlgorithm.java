package com.graphrag.core.algorithm;

import com.graphrag.core.service.EmbeddingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询理解和意图识别算法
 */
@Component
public class QueryUnderstandingAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(QueryUnderstandingAlgorithm.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingService embeddingService;

    // 查询分析提示模板
    private static final PromptTemplate QUERY_ANALYSIS_TEMPLATE = PromptTemplate.from("""
            请分析以下用户查询，提取关键信息：
            
            查询：{{query}}
            
            请按照以下格式输出：
            
            查询类型：[事实查询/概念解释/比较分析/推理问答/列表查询/其他]
            关键实体：[实体1, 实体2, ...]
            查询意图：[简要描述用户的查询意图]
            相关概念：[概念1, 概念2, ...]
            查询复杂度：[简单/中等/复杂]
            期望答案类型：[简短回答/详细解释/列表/比较表格/其他]
            
            注意：请确保提取的信息准确且有用。
            """);

    /**
     * 分析查询
     */
    public QueryAnalysis analyzeQuery(String query) {
        logger.debug("开始分析查询: {}", query);

        try {
            // 1. 使用 LLM 进行查询分析
            Prompt prompt = QUERY_ANALYSIS_TEMPLATE.apply(Map.of("query", query));
            String response = chatLanguageModel.generate(prompt.text());

            // 2. 解析 LLM 响应
            QueryAnalysis analysis = parseQueryAnalysis(response, query);

            // 3. 补充分析信息
            enhanceAnalysis(analysis, query);

            logger.info("查询分析完成，类型: {}, 复杂度: {}", analysis.getQueryType(), analysis.getComplexity());
            return analysis;

        } catch (Exception e) {
            logger.error("查询分析失败", e);
            return createFallbackAnalysis(query);
        }
    }

    /**
     * 解析查询分析响应
     */
    private QueryAnalysis parseQueryAnalysis(String response, String originalQuery) {
        QueryAnalysis analysis = new QueryAnalysis(originalQuery);

        // 提取查询类型
        String queryType = extractField(response, "查询类型");
        analysis.setQueryType(queryType != null ? queryType : "其他");

        // 提取关键实体
        String entitiesStr = extractField(response, "关键实体");
        if (entitiesStr != null) {
            List<String> entities = parseList(entitiesStr);
            analysis.setKeyEntities(entities);
        }

        // 提取查询意图
        String intent = extractField(response, "查询意图");
        analysis.setIntent(intent != null ? intent : "未知意图");

        // 提取相关概念
        String conceptsStr = extractField(response, "相关概念");
        if (conceptsStr != null) {
            List<String> concepts = parseList(conceptsStr);
            analysis.setRelatedConcepts(concepts);
        }

        // 提取查询复杂度
        String complexity = extractField(response, "查询复杂度");
        analysis.setComplexity(complexity != null ? complexity : "中等");

        // 提取期望答案类型
        String answerType = extractField(response, "期望答案类型");
        analysis.setExpectedAnswerType(answerType != null ? answerType : "详细解释");

        return analysis;
    }

    /**
     * 提取字段值
     */
    private String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + "：\\s*\\[?([^\\]\\n]+)\\]?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 解析列表
     */
    private List<String> parseList(String listStr) {
        if (listStr == null || listStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(listStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 增强分析信息
     */
    private void enhanceAnalysis(QueryAnalysis analysis, String query) {
        // 1. 检测查询模式
        analysis.setQueryPatterns(detectQueryPatterns(query));

        // 2. 提取时间信息
        analysis.setTemporalInfo(extractTemporalInfo(query));

        // 3. 检测比较意图
        analysis.setComparative(detectComparative(query));

        // 4. 计算查询向量
        List<Double> queryVector = embeddingService.embedText(query);
        analysis.setQueryVector(queryVector);

        // 5. 生成扩展查询
        analysis.setExpandedQueries(generateExpandedQueries(analysis));
    }

    /**
     * 检测查询模式
     */
    private List<String> detectQueryPatterns(String query) {
        List<String> patterns = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        // 疑问词模式
        if (lowerQuery.matches(".*\\b(什么|什么是|何为|如何|怎么|为什么|哪个|哪些|谁|何时|何地)\\b.*")) {
            patterns.add("疑问词查询");
        }

        // 定义模式
        if (lowerQuery.matches(".*\\b(是什么|定义|含义|概念)\\b.*")) {
            patterns.add("定义查询");
        }

        // 比较模式
        if (lowerQuery.matches(".*\\b(比较|对比|区别|差异|相同|不同)\\b.*")) {
            patterns.add("比较查询");
        }

        // 列表模式
        if (lowerQuery.matches(".*\\b(列出|列举|有哪些|包括|种类)\\b.*")) {
            patterns.add("列表查询");
        }

        // 因果模式
        if (lowerQuery.matches(".*\\b(原因|导致|影响|结果|后果)\\b.*")) {
            patterns.add("因果查询");
        }

        // 过程模式
        if (lowerQuery.matches(".*\\b(步骤|过程|流程|方法|如何)\\b.*")) {
            patterns.add("过程查询");
        }

        return patterns;
    }

    /**
     * 提取时间信息
     */
    private Map<String, String> extractTemporalInfo(String query) {
        Map<String, String> temporalInfo = new HashMap<>();

        // 检测时间表达式
        Pattern timePattern = Pattern.compile("\\b(\\d{4})年|\\b(\\d{1,2})月|\\b(\\d{1,2})日|\\b(今天|昨天|明天|最近|现在|当前|过去|未来)\\b");
        Matcher matcher = timePattern.matcher(query);

        while (matcher.find()) {
            String timeExpr = matcher.group();
            if (timeExpr.contains("年")) {
                temporalInfo.put("year", timeExpr.replace("年", ""));
            } else if (timeExpr.contains("月")) {
                temporalInfo.put("month", timeExpr.replace("月", ""));
            } else if (timeExpr.contains("日")) {
                temporalInfo.put("day", timeExpr.replace("日", ""));
            } else {
                temporalInfo.put("relative", timeExpr);
            }
        }

        return temporalInfo;
    }

    /**
     * 检测比较意图
     */
    private boolean detectComparative(String query) {
        String lowerQuery = query.toLowerCase();
        String[] comparativeWords = {"比较", "对比", "区别", "差异", "相同", "不同", "优缺点", "vs", "和", "与"};
        
        return Arrays.stream(comparativeWords)
                .anyMatch(lowerQuery::contains);
    }

    /**
     * 生成扩展查询
     */
    private List<String> generateExpandedQueries(QueryAnalysis analysis) {
        List<String> expandedQueries = new ArrayList<>();
        String originalQuery = analysis.getOriginalQuery();

        // 基于关键实体生成扩展查询
        for (String entity : analysis.getKeyEntities()) {
            expandedQueries.add(entity + "的定义");
            expandedQueries.add(entity + "的特点");
            expandedQueries.add(entity + "的应用");
        }

        // 基于相关概念生成扩展查询
        for (String concept : analysis.getRelatedConcepts()) {
            expandedQueries.add(concept + "与" + String.join("", analysis.getKeyEntities()) + "的关系");
        }

        // 基于查询类型生成扩展查询
        switch (analysis.getQueryType()) {
            case "概念解释":
                expandedQueries.add(originalQuery + "的详细解释");
                expandedQueries.add(originalQuery + "的例子");
                break;
            case "比较分析":
                expandedQueries.add(originalQuery + "的优缺点");
                expandedQueries.add(originalQuery + "的相似点");
                break;
            case "推理问答":
                expandedQueries.add(originalQuery + "的原因");
                expandedQueries.add(originalQuery + "的影响");
                break;
        }

        return expandedQueries.stream()
                .distinct()
                .limit(10)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 创建备用分析
     */
    private QueryAnalysis createFallbackAnalysis(String query) {
        QueryAnalysis analysis = new QueryAnalysis(query);
        analysis.setQueryType("其他");
        analysis.setIntent("一般查询");
        analysis.setComplexity("中等");
        analysis.setExpectedAnswerType("详细解释");
        analysis.setKeyEntities(extractSimpleEntities(query));
        analysis.setQueryPatterns(List.of("一般查询"));
        
        return analysis;
    }

    /**
     * 简单实体提取
     */
    private List<String> extractSimpleEntities(String query) {
        // 简单的实体提取逻辑，基于常见模式
        List<String> entities = new ArrayList<>();
        
        // 提取可能的实体（大写开头的词组）
        Pattern entityPattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        Matcher matcher = entityPattern.matcher(query);
        
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        return entities;
    }

    /**
     * 查询分析结果类
     */
    public static class QueryAnalysis {
        private final String originalQuery;
        private String queryType;
        private List<String> keyEntities = new ArrayList<>();
        private String intent;
        private List<String> relatedConcepts = new ArrayList<>();
        private String complexity;
        private String expectedAnswerType;
        private List<String> queryPatterns = new ArrayList<>();
        private Map<String, String> temporalInfo = new HashMap<>();
        private boolean comparative;
        private List<Double> queryVector;
        private List<String> expandedQueries = new ArrayList<>();

        public QueryAnalysis(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        // Getters and Setters
        public String getOriginalQuery() { return originalQuery; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public List<String> getKeyEntities() { return keyEntities; }
        public void setKeyEntities(List<String> keyEntities) { this.keyEntities = keyEntities; }
        
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        
        public List<String> getRelatedConcepts() { return relatedConcepts; }
        public void setRelatedConcepts(List<String> relatedConcepts) { this.relatedConcepts = relatedConcepts; }
        
        public String getComplexity() { return complexity; }
        public void setComplexity(String complexity) { this.complexity = complexity; }
        
        public String getExpectedAnswerType() { return expectedAnswerType; }
        public void setExpectedAnswerType(String expectedAnswerType) { this.expectedAnswerType = expectedAnswerType; }
        
        public List<String> getQueryPatterns() { return queryPatterns; }
        public void setQueryPatterns(List<String> queryPatterns) { this.queryPatterns = queryPatterns; }
        
        public Map<String, String> getTemporalInfo() { return temporalInfo; }
        public void setTemporalInfo(Map<String, String> temporalInfo) { this.temporalInfo = temporalInfo; }
        
        public boolean isComparative() { return comparative; }
        public void setComparative(boolean comparative) { this.comparative = comparative; }
        
        public List<Double> getQueryVector() { return queryVector; }
        public void setQueryVector(List<Double> queryVector) { this.queryVector = queryVector; }
        
        public List<String> getExpandedQueries() { return expandedQueries; }
        public void setExpandedQueries(List<String> expandedQueries) { this.expandedQueries = expandedQueries; }

        @Override
        public String toString() {
            return String.format("QueryAnalysis{query='%s', type='%s', intent='%s', complexity='%s'}", 
                    originalQuery, queryType, intent, complexity);
        }
    }
}

