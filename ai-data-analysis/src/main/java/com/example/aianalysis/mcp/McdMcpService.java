package com.example.aianalysis.mcp;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 麦当劳 MCP 服务
 *
 * 接入麦当劳 MCP 平台，提供以下能力：
 * 1. 活动日历查询
 * 2. 优惠券查询/领取
 * 3. 餐品营养信息查询
 * 4. 门店信息查询
 * 5. 订单管理
 *
 * 文档：https://open.mcd.cn/mcp/doc
 */
@Slf4j
@Service
public class McdMcpService {

    @Value("${mcd.mcp.base-url:https://mcp.mcd.cn}")
    private String baseUrl;

    @Value("${mcd.mcp.token:}")
    private String mcpToken;

    @Value("${mcd.api.app-id:}")
    private String appId;

    @Value("${mcd.api.merchant-id:}")
    private String merchantId;

    @Value("${mcd.api.key:}")
    private String apiKey;

    @Value("${mcd.api.env:PROD}")
    private String env;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取当前服务器时间
     */
    public McpResult<ServerTimeResponse> getServerTime() {
        log.info("获取麦当劳服务器时间");
        return callMcpTool("now-time-info", Map.of());
    }

    /**
     * 调用 MCP 工具
     */
    @SuppressWarnings("unchecked")
    private <T> McpResult<T> callMcpTool(String toolName, Map<String, Object> params) {
        if (mcpToken == null || mcpToken.isEmpty()) {
            log.warn("麦当劳 MCP Token 未配置");
            return McpResult.<T>builder()
                    .success(false)
                    .errorMessage("MCP Token 未配置，请在配置文件中设置 mcd.mcp.token")
                    .build();
        }

        try {
            // MCP Streamable HTTP 协议端点
            String url = baseUrl + "/mcp-servers/mcd-mcp";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + mcpToken);

            // MCP 协议请求体格式
            Map<String, Object> body = Map.of(
                    "jsonrpc", "2.0",
                    "method", "tools/call",
                    "params", Map.of(
                            "name", toolName,
                            "arguments", params
                    ),
                    "id", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.debug("调用 MCP 工具: {}, URL: {}", toolName, url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();

                // 检查是否有错误
                if (result.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) result.get("error");
                    return McpResult.<T>builder()
                            .success(false)
                            .errorMessage((String) error.get("message"))
                            .build();
                }

                // 解析 result 中的内容
                Map<String, Object> resultData = (Map<String, Object>) result.get("result");
                if (resultData != null && resultData.containsKey("content")) {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) resultData.get("content");
                    // 提取文本内容
                    String textContent = content.stream()
                            .filter(c -> "text".equals(c.get("type")))
                            .map(c -> (String) c.get("text"))
                            .findFirst()
                            .orElse("");

                    return McpResult.<T>builder()
                            .success(true)
                            .data((T) textContent)
                            .build();
                }

                return McpResult.<T>builder()
                        .success(true)
                        .data((T) resultData)
                        .build();
            } else {
                return McpResult.<T>builder()
                        .success(false)
                        .errorMessage("HTTP " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("调用 MCP 工具失败: {}", toolName, e);
            return McpResult.<T>builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    // ============ 签名相关（用于直接调用开放平台 API） ============

    /**
     * 生成签名
     */
    public String generateSign(String appId, String merchantId, String body, long timestamp, String key) {
        String signStr = String.format("AppId=%s&Body=%s&MerchantId=%s&Timestamp=%s&key=%s",
                appId, body, merchantId, timestamp, key);
        return md5(signStr).toUpperCase();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    // ============ DTO 定义 ============

    @Data
    @Builder
    public static class McpResult<T> {
        private boolean success;
        private T data;
        private String errorMessage;
    }

    // 响应数据类（简化定义，实际根据 API 返回调整）
    @Data
    public static class ServerTimeResponse {
        private String serverTime;
        private long timestamp;
    }

    @Data
    public static class ActivityCalendarResponse {
        private List<Activity> activities;
    }

    @Data
    public static class Activity {
        private String id;
        private String title;
        private String description;
        private String startDate;
        private String endDate;
        private String type;
    }

    @Data
    public static class CouponListResponse {
        private List<Coupon> coupons;
    }

    @Data
    public static class Coupon {
        private String couponId;
        private String name;
        private String description;
        private String validStartTime;
        private String validEndTime;
        private int status;
    }

    @Data
    public static class ClaimCouponsResponse {
        private int successCount;
        private int failCount;
        private List<String> failedCouponIds;
    }

    @Data
    public static class UserCouponListResponse {
        private List<UserCoupon> coupons;
    }

    @Data
    public static class UserCoupon {
        private String couponCode;
        private String couponName;
        private String validStartTime;
        private String validEndTime;
        private int availableCount;
        private int totalCount;
    }

    @Data
    public static class NutritionInfoResponse {
        private String productName;
        private int calories;
        private int protein;
        private int fat;
        private int carbohydrates;
        private int sodium;
    }

    @Data
    public static class StoreListResponse {
        private List<Store> stores;
    }

    @Data
    public static class Store {
        private String storeCode;
        private String storeName;
        private String address;
        private double latitude;
        private double longitude;
        private String phone;
        private String businessHours;
    }

    @Data
    public static class CityListResponse {
        private List<City> cities;
    }

    @Data
    public static class City {
        private String cityCode;
        private String cityName;
        private String provinceCode;
        private String provinceName;
    }
}
