package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.AnomalyReport;
import com.salary.service.AnomalyReportService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "Anomaly Report")
@RestController
@RequestMapping("/anomaly")
@RequiredArgsConstructor
public class AnomalyReportController {

    private final AnomalyReportService anomalyService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list anomaly reports")
    @GetMapping("/page")
    public Result<PageResult<AnomalyReport>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer reportType,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long reporterId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null; // admin(1) sees all, manager(2) sees own
        return Result.success(anomalyService.page(current, size, reportType, status, reporterId));
    }

    @ApiOperation("Submit an anomaly report")
    @PostMapping
    public Result<Void> submit(@RequestBody AnomalyReport report, HttpServletRequest request) {
        Long reporterId = Long.valueOf(claims(request).get("userId").toString());
        report.setReporterId(reporterId);
        report.setStatus(0);
        anomalyService.save(report);
        return Result.successMsg("Submitted");
    }

    @ApiOperation("Process an anomaly report (admin/hr only)")
    @PutMapping("/{id}/process")
    public Result<Void> process(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String processResult,
            HttpServletRequest request) {
        Long processorId = Long.valueOf(claims(request).get("userId").toString());
        anomalyService.processReport(id, status, processResult, processorId);
        return Result.successMsg("Processed");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }
}
