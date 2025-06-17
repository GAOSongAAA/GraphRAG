package com.graphrag.core.prompt;

import dev.langchain4j.model.input.PromptTemplate;

public final class RAGPromptTemplates {

        private RAGPromptTemplates() {
        }

        /**
         * Template used for answering user questions with provided context.
         */
        public static final PromptTemplate RAG_TEMPLATE = PromptTemplate.from(
                        """
                                        Answer the user's question based on the following context information.\n\n" +
                                        "Context:\n" +
                                        "{{context}}\n\n" +
                                        "User Question:\n" +
                                        "{{question}}\n\n" +
                                        "Please provide accurate and detailed answers based on the context. If the context is insufficient to answer the question, please indicate this.\n\n" +
                                        "Answer:\n
                                        """);
}