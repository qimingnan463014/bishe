package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.entity.Performance;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.PerformanceService;
import com.salary.service.SysLogService;
import com.salary.util.ExcelAvatarExportUtil;
import com.salary.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    @ApiOperation("导出绩效 Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String yearMonth,
                       @RequestParam(required = false) String empName,
                       @RequestParam(required = false) Long deptId,
                       @RequestParam(required = false) String managerName,
                       HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long managerId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null;
        java.util.List<Performance> records = performanceService.page(1, 50000, yearMonth, null, deptId, managerId).getRecords();
        Map<Long, Employee> employeeById = new HashMap<>();
        Map<String, Employee> employeeByNo = new HashMap<>();
        for (Employee employee : employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1))) {
            employeeById.put(employee.getId(), employee);
            employeeByNo.put(employee.getEmpNo(), employee);
        }
        records.removeIf(row -> !matchKeyword(ExcelAvatarExportUtil.firstNonBlank(row.getEmpName(), resolveEmployee(row, employeeById, employeeByNo).getRealName(), "-"), empName)
                || !matchKeyword(ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), "-"), managerName));
        ExcelAvatarExportUtil.export(
                response,
                "绩效评分.xlsx",
                "绩效评分",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("姓名", 12, row -> ExcelAvatarExportUtil.firstNonBlank(row.getEmpName(), resolveEmployee(row, employeeById, employeeByNo).getRealName(), "-")),
                        ExcelAvatarExportUtil.avatar("头像", 12, row -> resolveEmployee(row, employeeById, employeeByNo).getAvatar()),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(resolveEmployee(row, employeeById, employeeByNo).getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("绩效月份", 12, Performance::getYearMonth),
                        ExcelAvatarExportUtil.text("工作态度", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getWorkAttitude())),
                        ExcelAvatarExportUtil.text("业务技能", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getBusinessSkill())),
                        ExcelAvatarExportUtil.text("工作绩效", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getWorkPerformance())),
                        ExcelAvatarExportUtil.text("奖惩加减分", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getBonusDeduct())),
                        ExcelAvatarExportUtil.text("总得分", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getScore())),
                        ExcelAvatarExportUtil.text("评价等级", 12, Performance::getGrade),
                        ExcelAvatarExportUtil.text("添加时间", 14, row -> ExcelAvatarExportUtil.formatDate(row.getCreateTime())),
                        ExcelAvatarExportUtil.text("部门经理", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), "-")),
                        ExcelAvatarExportUtil.text("评审评语", 24, row -> ExcelAvatarExportUtil.firstNonBlank(row.getEvalComment(), "-"))
                ),
                records
        );
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

    private Employee resolveEmployee(Performance row,
                                     Map<Long, Employee> employeeById,
                                     Map<String, Employee> employeeByNo) {
        if (row.getEmpId() != null && employeeById.containsKey(row.getEmpId())) {
            return employeeById.get(row.getEmpId());
        }
        if (row.getEmpNo() != null && employeeByNo.containsKey(row.getEmpNo())) {
            return employeeByNo.get(row.getEmpNo());
        }
        return new Employee();
    }

    private boolean matchKeyword(String source, String keyword) {
        return !org.springframework.util.StringUtils.hasText(keyword)
                || (source != null && source.contains(keyword));
    }
}
