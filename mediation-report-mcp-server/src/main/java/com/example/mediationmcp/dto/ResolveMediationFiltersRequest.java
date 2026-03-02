package com.example.mediationmcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
public class ResolveMediationFiltersRequest {

    @JsonPropertyDescription("应用名称列表。若提供，将尝试从数据库映射为 appKey")
    private List<String> appNames;

    @JsonPropertyDescription("包名列表。若提供，将尝试从数据库映射为 appKey")
    private List<String> pkgNames;

    @JsonPropertyDescription("广告类型文案列表，例如 Native、Banner。若提供，将映射为 adType 数值")
    private List<String> adTypeLabels;

    @JsonPropertyDescription("已是标准值的 appKey 列表，会与映射结果合并去重")
    private List<String> appKey;

    @JsonPropertyDescription("已是标准值的 adType 列表，会与映射结果合并去重")
    private List<Integer> adType;
}
