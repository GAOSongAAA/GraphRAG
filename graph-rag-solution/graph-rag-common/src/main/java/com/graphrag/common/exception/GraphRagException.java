package com.graphrag.common.exception;

/**
 * 图 RAG 业务异常基类
 */
public class GraphRagException extends RuntimeException {

    private final String errorCode;

    public GraphRagException(String message) {
        super(message);
        this.errorCode = "GRAPH_RAG_ERROR";
    }

    public GraphRagException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GraphRagException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GRAPH_RAG_ERROR";
    }

    public GraphRagException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

