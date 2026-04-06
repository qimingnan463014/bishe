package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.entity.Performance;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.PerformanceService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Performance Controller
 *
 * Auto-scoring logic enforced in PerformanceServiceImpl:
 *   totalScore = avg(workAttitude, businessSkill, workPerformance) + bonusDeduct
 *   grade mapping:
 *     >= 90 -> Excellent, perfBonusRatio = 1.2
 *     >= 80 -> Good,      perfBonusRatio = 1.0
 *     >= 60 -> Normal,    perfBonusRatio = 0.8
 *     <  60 -> Poor,      perfBonusRatio = 0.0
 *   After saving, syncs perfBonus to the salary draft record if one exists.
 */
@Api(tags = "Performance Management")
@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;
    private final EmployeeMapper employeeMapper;

    @ApiOperation("Page query performance records (admin=all, manager=own dept)")
    @GetMapping("/page")
    public Result<PageResult<Performance>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long managerId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null;
        return Result.success(performanceService.page(current, size, yearMonth, empNo, deptId, managerId));
    }

    @ApiOperation("Get performance detail by id")
    @GetMapping("/{id}")
    public Result<Performance> detail(@PathVariable Long id) {
        return Result.success(performanceService.getDetail(id));
    }

    @ApiOperation("Employee: my performance history (status >= 2)")
    @GetMapping("/my/history")
    public Result<PageResult<Performance>> myHistory(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims(request).get("userId").toString()));
        if (employee == null) {
            return Result.success(PageResult.of(new Page<>(current, size)));
        }
        Long empId = employee.getId();
        return Result.success(performanceService.getMyPerformance(current, size, empId));
    }

    /**
     * Upsert: if same emp+month exists -> update, otherwise -> insert.
     * Auto-calculates totalScore, grade, perfBonusRatio.
     * Syncs perfBonus to salary record if already generated.
     */
    @ApiOperation("Save or update performance score (idempotent, auto grade)")
    @PostMapping("/save")
    public Result<Void> saveOrUpdate(
            @RequestBody Performance perf,
            HttpServletRequest request) {
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        performanceService.saveOrUpdateScore(perf, managerId);
        return Result.successMsg("Saved");
    }

    @ApiOperation("Add new performance record (auto grade + sync salary bonus)")
    @PostMapping
    public Result<Performance> add(
            @RequestBody Performance perf,
            HttpServletRequest request) {
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        return Result.success(performanceService.addPerformance(perf, managerId));
    }

    @ApiOperation("Update performance (re-calculates grade + syncs salary bonus)")
    @PutMapping
    public Result<Void> update(@RequestBody Performance perf) {
        performanceService.updatePerformance(perf);
        return Result.successMsg("Updated");
    }

    @ApiOperation("Confirm performance: submitted(2) -> confirmed(3), immutable after")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        performanceService.confirm(id);
        return Result.successMsg("Confirmed");
    }

    @ApiOperation("Delete performance")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Performance performance = performanceService.getById(id);
        if (performance == null) {
            return Result.error("绩效记录不存在");
        }
        performanceService.removeById(id);
        Claims claims = claims(request);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "绩效评分",
                "删除绩效记录[" + (performance.getEmpName() == null ? "-" : performance.getEmpName()) + "/" +
                        (performance.getYearMonth() == null ? "-" : performance.getYearMonth()) + "]");
        return Result.successMsg("Deleted");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }
}
