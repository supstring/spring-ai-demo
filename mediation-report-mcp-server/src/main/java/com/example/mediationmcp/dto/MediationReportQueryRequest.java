package com.example.mediationmcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MediationReportQueryRequest {

    @JsonPropertyDescription("时区，必填，未指定默认8。可选值：8(UTC+8)、0(UTC)、-8(UTC-8)")
    private Integer timeZone;
    @JsonPropertyDescription("开始日期，必填，格式 yyyy-MM-dd")
    private String startDate;
    @JsonPropertyDescription("结束日期，必填，格式 yyyy-MM-dd")
    private String endDate;
    @JsonPropertyDescription("开始小时，格式 HH，例如 00")
    private String startHour;
    @JsonPropertyDescription("结束小时，格式 HH，例如 23")
    private String endHour;
    @JsonPropertyDescription("时间聚合方式，必填：0=整体，1=分时，2=分日，3=分月，4=季度，5=周")
    private Integer aggregateType;
    @JsonPropertyDescription("分页页码。未指定时默认 1")
    private Integer pageNo;
    @JsonPropertyDescription("每页数量。未指定时默认 20")
    private Integer pageSize;

    @JsonPropertyDescription("分组维度列表，例如 country、developerId、adType、dspName、appKey。为空表示整体统计")
    private List<String> breakDowns;

    @NotEmpty(message = "indicators must contain at least one metric")
    @JsonPropertyDescription("查询指标列表，至少包含一个指标，例如 eincome、ecpm、adReqCnt")
    private List<String> indicators;

    @JsonPropertyDescription("广告位 ID 过滤")
    private List<String> slotId;
    @JsonPropertyDescription("广告类型过滤")
    private List<Integer> adType;
    @JsonPropertyDescription("国家过滤，例如 AU、US")
    private List<String> country;
    @JsonPropertyDescription("包名过滤")
    private List<String> pkgName;
    @JsonPropertyDescription("ADX 类型过滤")
    private List<Integer> adxType;
    @JsonPropertyDescription("DSP 名称过滤，例如 BigoDsp")
    private List<String> dspName;
    @JsonPropertyDescription("系统过滤，例如 iOS、Android")
    private List<String> appOs;
    @JsonPropertyDescription("开发者 ID 过滤")
    private List<String> developerId;
    @JsonPropertyDescription("AppKey 过滤")
    private List<String> appKey;
    @JsonPropertyDescription("竞价类型过滤")
    private List<Integer> auctionType;
    @JsonPropertyDescription("样式 ID 过滤")
    private List<Integer> styleId;
    @JsonPropertyDescription("分类过滤")
    private List<String> category;
    @JsonPropertyDescription("自定义分类 ID 过滤")
    private List<Integer> customizeCategoryId;
    @JsonPropertyDescription("SDK 版本过滤")
    private List<String> sdkVer;
    @JsonPropertyDescription("商务对接人 ID 过滤")
    private List<Integer> businessLiaison;
    @JsonPropertyDescription("运营对接人 ID 过滤")
    private List<Integer> operateLiaison;
    @JsonPropertyDescription("域名过滤")
    private List<String> domain;
    @JsonPropertyDescription("账户 ID 过滤")
    private List<Integer> accountId;
    @JsonPropertyDescription("DSP 组织名称过滤")
    private List<String> dspOrganizationName;
    @JsonPropertyDescription("服务区域过滤")
    private List<Integer> serverRegion;
}
