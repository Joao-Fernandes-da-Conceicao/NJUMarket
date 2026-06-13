package com.njumarket.admin.controller;

import com.njumarket.njumarket.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Elasticsearch 管理控制器
 * 提供 ES 索引管理功能，代理调用 commodity 服务的内部接口
 */
@Slf4j
@Tag(name = "Elasticsearch 管理", description = "Elasticsearch 索引管理相关接口")
@RestController
@RequestMapping("/api/admin/elasticsearch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM')") // 只有 system 管理员可以管理 ES
public class ElasticsearchController {

    private final RestTemplate restTemplate;

    // 通过服务名调用（使用 Eureka 服务发现）
    // 格式：http://服务名:端口
    @Value("${njumarket.service.commodity.url:http://njumarket-service-trade:8092}")
    private String commodityServiceUrl;

    @Operation(summary = "重建搜索索引", description = "全量重建商品搜索索引，建议在低峰期执行")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重建成功"),
        @ApiResponse(responseCode = "500", description = "重建失败")
    })
    @PostMapping("/reindex")
    public Result rebuildIndex() {
        try {
            log.info("管理员请求重建搜索索引");
            String url = commodityServiceUrl + "/api/internal/commodity/search/reindex";
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Result> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Result.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Result result = response.getBody();
                log.info("搜索索引重建成功: {}", result.getData());
                return result;
            } else {
                log.error("搜索索引重建失败: 响应状态码={}", response.getStatusCode());
                return Result.fail("搜索索引重建失败");
            }
        } catch (Exception e) {
            log.error("搜索索引重建异常: {}", e.getMessage(), e);
            return Result.fail("搜索索引重建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步商品到搜索索引", description = "将指定商品同步到搜索索引")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "同步成功"),
        @ApiResponse(responseCode = "404", description = "商品不存在"),
        @ApiResponse(responseCode = "500", description = "同步失败")
    })
    @PostMapping("/sync/{commodityId}")
    public Result syncCommodity(
        @Parameter(description = "商品ID", required = true)
        @PathVariable String commodityId) {
        try {
            log.info("管理员请求同步商品到搜索索引: commodityId={}", commodityId);
            String url = commodityServiceUrl + "/api/internal/commodity/" + commodityId + "/search-sync";
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Result> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Result.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Result result = response.getBody();
                log.info("商品同步成功: commodityId={}", commodityId);
                return result;
            } else {
                log.error("商品同步失败: commodityId={}, 响应状态码={}", commodityId, response.getStatusCode());
                return Result.fail("商品同步失败");
            }
        } catch (Exception e) {
            log.error("商品同步异常: commodityId={}, error={}", commodityId, e.getMessage(), e);
            return Result.fail("商品同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取索引统计信息", description = "获取搜索索引的统计信息（索引文档数等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/stats")
    public Result getIndexStats() {
        try {
            // 这里可以扩展为调用 ES 的统计接口
            // 目前返回基本信息
            return Result.ok("获取成功", Map.of(
                "message", "索引统计功能待实现",
                "note", "可以通过 ES 的 _cat/indices API 获取索引统计信息"
            ));
        } catch (Exception e) {
            log.error("获取索引统计信息异常: {}", e.getMessage(), e);
            return Result.fail("获取索引统计信息失败: " + e.getMessage());
        }
    }
}

