package com.asamu.aiagent.controller;

import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.metrics.AppMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MetricsController {

    private final AppMetricsService metricsService;

    public MetricsController(AppMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Long>> metrics() {
        return ApiResponse.ok(metricsService.snapshot());
    }
}
