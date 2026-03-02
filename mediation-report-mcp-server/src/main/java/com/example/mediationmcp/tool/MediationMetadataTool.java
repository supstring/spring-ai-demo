package com.example.mediationmcp.tool;

import com.example.mediationmcp.dto.ResolveMediationFiltersRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediationMetadataTool {

    private static final Map<Integer, String> AD_TYPE_ENUM = Map.of(
            1, "Native",
            2, "Banner"
    );

    private static final Map<String, Integer> AD_TYPE_LABEL_TO_CODE = AD_TYPE_ENUM.entrySet()
            .stream()
            .collect(Collectors.toMap(
                    entry -> entry.getValue().toLowerCase(),
                    Map.Entry::getKey
            ));

    private static final List<String> COMMON_INDICATORS = List.of(
            "eincome", "beforeEincome", "profit", "ecpm", "rpm", "adReqCnt", "adImprCnt", "adClickCnt", "adCtr"
    );

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    @Value("${app.lookup.app-key-by-name-sql:SELECT app_key FROM mediation_app WHERE app_name = ?}")
    private String appKeyByNameSql;

    @Value("${app.lookup.app-key-by-pkg-sql:SELECT app_key FROM mediation_app WHERE pkg_name = ?}")
    private String appKeyByPkgSql;

    @Tool(name = "get_mediation_metadata", description = """
返回 query_mediation_report 所需的字段元数据、枚举说明和推荐调用方式。
当用户输入包含文案值（如 adType=Banner）或业务名称（如应用名称）时，应先调用此工具了解映射规则。
""")
    public Map<String, Object> getMediationMetadata() {
        Map<String, Object> enumFields = new LinkedHashMap<>();
        enumFields.put("adType", AD_TYPE_ENUM);

        Map<String, Object> resolveRules = new LinkedHashMap<>();
        resolveRules.put("appName", "先通过 resolve_mediation_filters 查询数据库映射到 appKey");
        resolveRules.put("pkgName", "先通过 resolve_mediation_filters 查询数据库映射到 appKey");
        resolveRules.put("adTypeLabels", "通过内置字典映射为 adType 数值（如 Banner -> 2）");

        return Map.of(
                "tool", "query_mediation_report",
                "enumFields", enumFields,
                "commonIndicators", COMMON_INDICATORS,
                "supportedBreakDowns", List.of("country", "developerId", "adType", "dspName", "appKey"),
                "recommendedWorkflow", List.of(
                        "1) 先调用 resolve_mediation_filters 将名称/文案解析为标准过滤值",
                        "2) 再调用 query_mediation_report 进行报表查询"
                ),
                "resolveRules", resolveRules
        );
    }

    @Tool(name = "resolve_mediation_filters", description = """
将自然语言过滤条件转换为 query_mediation_report 可直接使用的标准过滤参数。
支持：
1) adType 文案到枚举值映射（如 Native -> 1, Banner -> 2）
2) 应用名称或包名到 appKey 的数据库查询映射
""")
    public Map<String, Object> resolveMediationFilters(ResolveMediationFiltersRequest request) {
        if (request == null) {
            return Map.of(
                    "success", false,
                    "errorCode", "EMPTY_REQUEST",
                    "message", "request is required"
            );
        }

        Set<String> resolvedAppKeys = new LinkedHashSet<>();
        Set<Integer> resolvedAdTypes = new LinkedHashSet<>();
        Map<String, Object> unresolved = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();

        addAll(resolvedAppKeys, request.getAppKey());
        addAll(resolvedAdTypes, request.getAdType());

        List<String> unresolvedAdTypeLabels = resolveAdTypeLabels(request.getAdTypeLabels(), resolvedAdTypes);
        if (!unresolvedAdTypeLabels.isEmpty()) {
            unresolved.put("adTypeLabels", unresolvedAdTypeLabels);
        }

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            if (!CollectionUtils.isEmpty(request.getAppNames()) || !CollectionUtils.isEmpty(request.getPkgNames())) {
                notes.add("未检测到 DataSource/JdbcTemplate，无法执行 appName/pkgName 到 appKey 的数据库映射");
                unresolved.put("appNames", normalizeStrings(request.getAppNames()));
                unresolved.put("pkgNames", normalizeStrings(request.getPkgNames()));
            }
        } else {
            resolveAppKeysByNames(jdbcTemplate, request.getAppNames(), resolvedAppKeys, unresolved);
            resolveAppKeysByPkgNames(jdbcTemplate, request.getPkgNames(), resolvedAppKeys, unresolved);
        }

        Map<String, Object> resolvedFilters = new LinkedHashMap<>();
        if (!resolvedAppKeys.isEmpty()) {
            resolvedFilters.put("appKey", resolvedAppKeys);
        }
        if (!resolvedAdTypes.isEmpty()) {
            resolvedFilters.put("adType", resolvedAdTypes);
        }

        boolean hasUnresolved = !unresolved.isEmpty();
        return Map.of(
                "success", !hasUnresolved,
                "resolvedFilters", resolvedFilters,
                "unresolved", unresolved,
                "adTypeEnum", AD_TYPE_ENUM,
                "notes", notes
        );
    }

    private List<String> resolveAdTypeLabels(List<String> labels, Set<Integer> resolvedAdTypes) {
        if (CollectionUtils.isEmpty(labels)) {
            return List.of();
        }
        List<String> unresolved = new ArrayList<>();
        for (String label : labels) {
            if (!StringUtils.hasText(label)) {
                continue;
            }
            Integer code = AD_TYPE_LABEL_TO_CODE.get(label.trim().toLowerCase());
            if (code == null) {
                unresolved.add(label);
                continue;
            }
            resolvedAdTypes.add(code);
        }
        return unresolved;
    }

    private void resolveAppKeysByNames(
            JdbcTemplate jdbcTemplate,
            List<String> appNames,
            Set<String> resolvedAppKeys,
            Map<String, Object> unresolved) {
        if (CollectionUtils.isEmpty(appNames)) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String appName : normalizeStrings(appNames)) {
            List<String> appKeys = queryAppKeys(jdbcTemplate, appKeyByNameSql, appName);
            if (appKeys.isEmpty()) {
                missing.add(appName);
                continue;
            }
            resolvedAppKeys.addAll(appKeys);
        }
        if (!missing.isEmpty()) {
            unresolved.put("appNames", missing);
        }
    }

    private void resolveAppKeysByPkgNames(
            JdbcTemplate jdbcTemplate,
            List<String> pkgNames,
            Set<String> resolvedAppKeys,
            Map<String, Object> unresolved) {
        if (CollectionUtils.isEmpty(pkgNames)) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String pkgName : normalizeStrings(pkgNames)) {
            List<String> appKeys = queryAppKeys(jdbcTemplate, appKeyByPkgSql, pkgName);
            if (appKeys.isEmpty()) {
                missing.add(pkgName);
                continue;
            }
            resolvedAppKeys.addAll(appKeys);
        }
        if (!missing.isEmpty()) {
            unresolved.put("pkgNames", missing);
        }
    }

    private List<String> queryAppKeys(JdbcTemplate jdbcTemplate, String sql, String value) {
        try {
            return jdbcTemplate.queryForList(sql, String.class, value)
                    .stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            log.error("resolve appKey failed for value={}", value, ex);
            return List.of();
        }
    }

    private <T> void addAll(Set<T> target, List<T> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        target.addAll(values);
    }

    private List<String> normalizeStrings(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
