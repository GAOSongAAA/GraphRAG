package com.graphrag.api.interceptor;

import com.graphrag.api.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 限流攔截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();

        // 根據不同的API路徑應用不同的限流策略
        Bucket bucket = getBucketForRequest(clientIp, uri);
        
        if (bucket != null) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                // 添加限流資訊到響應頭
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                return true;
            } else {
                // 限流觸發
                logger.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, uri);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + 
                        probe.getNanosToWaitForRefill() / 1_000_000_000 + "}");
                return false;
            }
        }

        return true;
    }

    /**
     * 根據請求獲取對應的限流桶
     */
    private Bucket getBucketForRequest(String clientIp, String uri) {
        if (uri.contains("/query")) {
            return rateLimitConfig.getIpBucket(clientIp + ":query");
        } else if (uri.contains("/upload")) {
            return rateLimitConfig.getIpBucket(clientIp + ":upload");
        } else {
            return rateLimitConfig.getIpBucket(clientIp);
        }
    }

    /**
     * 獲取客戶端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
