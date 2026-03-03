package com.example.mediationmcp.tool;

import com.example.mediationmcp.dto.MediationReportQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediationReportTool {

    @Value("${app.backend.base-url:http://localhost:10013}")
    private String backendBaseUrl;

    @Value("${app.backend.report-path:/mediation/report/sdk_developer_list_new}")
    private String reportPath;

    private final RestTemplate restTemplate;

    @Tool(name = "query_mediation_report", description = """
用于查询广告聚合平台的开发者维度报表数据。
适用于广告收入、ecpm、请求量、展示量、点击量等指标的统计分析。
支持按国家、开发者、DSP、广告类型等维度分组，支持趋势分析和分页查询。
分析「某日某范围数据为何下降」时，可先查整体再通过 breakDowns 下钻（如 dspName、adType、developerId、appKey）找出下降较多的子维度。

参数字段说明请参考请求对象 MediationReportQueryRequest。

==============================
指标字段说明（indicators）
==============================

indicators 必须填写系统支持的指标字段名称。

常见自然语言与字段的对应关系如下：

Revenue/收入/收益 --> beforeEincome
revenue(model) --> beforeEincomeModel
Cost/成本 --> eincome
Bidding Fee--> biddingFee
Profit --> profit
Share Rate --> eincomeRate
SDK Impressions --> adImprCnt
SDK Clicks--> adClickCnt
CTR --> adCtr
ADX CPM --> beforeEcpm
Developer CPM --> ecpm
ADX RPM--> adxRpm
Developer RPM --> rpm
DSP Requests RPM --> dspReqRpm
Developer CPC --> cpc
Developer Requests--> clientReqCnt
SDK Filledbids --> clientFillCnt
SDK Filledbids Rate --> clientFillRate
SDK Impressions Rate --> adImprRate
SDK Filledbids Diff--> clientFillBidSucGap
SDK Real Requests --> adReqCnt
SDK Real Requests Rate --> adReqRate
ADX Received Requests --> sspReqCnt
ADX Received Requests Diff--> sspReqCntAdReqCntGap
ADX Real Requests --> sspRealReqCnt
ADX Real Requests QPS --> adxRealReqQps
ADX Filledbids --> bidSucCnt
ADX Filledbids CPM--> averageClearingPrice
ADX Filledbids Rate --> bidSucRealReqRate
ADX Impressions Rate --> adxImprRate
DSP Requests --> realReqCnt
DSP Requests QPS--> realReqQps
DSP Responses --> resCnt
DSP Valid Responses --> validResCnt
DSP Valid Responses CPM --> validAverageBidPrice
DSP Valid Responses Rate--> validResRate
DSP Valid Responses WinRate --> bidSucRate
ad1展示 --> multiAdImprCnt1
ad1点击 --> multiAdClickCnt1
ad1收入--> multiAdRevenue1
ad2展示 --> multiAdImprCnt2
ad2点击 --> multiAdClickCnt2
ad2收入--> multiAdRevenue2

如果用户询问多个指标，可以同时填写多个，例如：

["eincome","ecpm","adImprCnt"]

==============================
可用指标列表
==============================

beforeEincome
profit
adCtr
rpm
clientFillCnt
sspRealReqCnt
adReqCnt
bidSucRealReqRate
resCnt
bidSucRate
multiAdImprCnt2
multiAdClickCnt2
multiAdImprCnt1
validResCnt
adxImprRate
adxRealReqQps
adReqRate
clientFillRate
dspReqRpm
beforeEcpm
eincomeRate
beforeEincomeModel
eincome
adImprCnt
ecpm
cpc
adImprRate
sspReqCnt
bidSucCnt
realReqCnt
validAverageBidPrice
multiAdClickCnt1
multiAdRevenue2
multiAdRevenue1
validResRate
realReqQps
averageClearingPrice
sspReqCntAdReqCntGap
clientFillBidSucGap
clientReqCnt
adxRpm
adClickCnt
biddingFee

至少必须填写一个指标。
""")
    public Map<String, Object> queryMediationReport(MediationReportQueryRequest request) {
        applyDefaults(request);
        validateRequest(request);

        String url = backendBaseUrl + reportPath;
        Map<String, Object> payload = toPayload(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USERNAME-X", "zhengzhirui");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getStatusCode().is2xxSuccessful());
            result.put("statusCode", response.getStatusCode().value());
            result.put("data", response.getBody());
            result.put("appliedDefaults", Map.of(
                    "pageNo", request.getPageNo(),
                    "pageSize", request.getPageSize(),
                    "aggregateType", request.getAggregateType(),
                    "breakDowns", request.getBreakDowns()
            ));
            return result;
        } catch (Exception ex) {
            log.error("query_mediation_report call failed", ex);
            return Map.of(
                    "success", false,
                    "error", ex.getMessage(),
                    "requestPayload", payload
            );
        }
    }

    private void applyDefaults(MediationReportQueryRequest request) {
        if (request.getPageNo() == null || request.getPageNo() <= 0) {
            request.setPageNo(1);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(20);
        }
        if (request.getAggregateType() == null) {
            request.setAggregateType(2);
        }
        if (request.getBreakDowns() == null) {
            request.setBreakDowns(List.of());
        }
    }

    private void validateRequest(MediationReportQueryRequest request) {
        if (request.getTimeZone() == null) {
            throw new IllegalArgumentException("timeZone is required");
        }
        if (!StringUtils.hasText(request.getStartDate()) || !StringUtils.hasText(request.getEndDate())) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (request.getIndicators() == null || request.getIndicators().isEmpty()) {
            throw new IllegalArgumentException("indicators must contain at least one metric");
        }
    }

    private Map<String, Object> toPayload(MediationReportQueryRequest request) {
        BeanWrapper wrapper = new BeanWrapperImpl(request);
        Map<String, Object> payload = new HashMap<>();
        for (var descriptor : wrapper.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if ("class".equals(name)) {
                continue;
            }
            Object value = wrapper.getPropertyValue(name);
            if (value != null) {
                payload.put(name, value);
            }
        }
        return payload;
    }
}
