package com.example.taskmanager.controller;

import com.example.taskmanager.dto.dashboard.DashboardStatsResponse;
import com.example.taskmanager.security.SecurityUtils;
import com.example.taskmanager.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard statistics for the authenticated user.
 *
 * <pre>GET /api/dashboard/stats</pre>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> stats() {
        return ResponseEntity.ok(dashboardService.getStats(SecurityUtils.currentUserId()));
    }
}
