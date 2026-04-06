package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.AnomalyReport;
import com.salary.entity.Employee;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.AnomalyReportService;
import com.salary.service.SysLogService;
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
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;
    private final EmployeeMapper employeeMapper;

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
        Long currentUserId = Long.valueOf(c.get("userId").toString());
        Long reporterId = null;
        Long empId = null;
        if (role == 2) {
            reporterId = currentUserId;
        } else if (role == 3) {
            Employee employee = employeeMapper.selectByUserId(currentUserId);
            empId = employee != null ? employee.getId() : -1L;
        }
        return Result.success(anomalyService.page(current, size, reportType, status, reporterId, empId));
    }

    @ApiOperation("Submit an anomaly report")
    @PostMapping
    public Result<Void> submit(@RequestBody AnomalyReport report, HttpServletRequest request) {
        Long reporterId = Long.valueOf(claims(request).get("userId").toString());
        Employee employee = employeeMapper.selectByUserId(reporterId);
        report.setReporterId(reporterId);
        if (employee != null) {
            report.setEmpId(employee.getId());
        }
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

    @ApiOperation("Delete an anomaly report")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AnomalyReport report = anomalyService.getById(id);
        if (report == null) {
            return Result.error("异常记录不存在");
        }
        anomalyService.removeById(id);
        Claims claims = claims(request);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "异常上报",
                "删除异常上报[" + (report.getTitle() == null ? "ID=" + id : report.getTitle()) + "/" +
                        (report.getYearMonth() == null ? "-" : report.getYearMonth()) + "]");
        return Result.successMsg("Deleted");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }
}
