package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.AttendanceApply;
import com.salary.entity.Employee;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.AttendanceApplyService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;

@Api(tags = "Attendance Apply")
@RestController
@RequestMapping("/attendance-apply")
@RequiredArgsConstructor
public class AttendanceApplyController {

    private final AttendanceApplyService applyService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;
    private final EmployeeMapper employeeMapper;

    @ApiOperation("Page list attendance applies")
    @GetMapping("/page")
    public Result<PageResult<AttendanceApply>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long managerId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null;
        if (role == 3) {
            Employee employee = employeeMapper.selectByUserId(Long.valueOf(c.get("userId").toString()));
            empId = employee != null ? employee.getId() : -1L;
        }
        return Result.success(applyService.page(current, size, empId, status, managerId));
    }

    @ApiOperation("Employee: my applies")
    @GetMapping("/my")
    public Result<PageResult<AttendanceApply>> myApplies(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims(request).get("userId").toString()));
        if (employee == null) {
            return Result.success(PageResult.of(new Page<>(current, size)));
        }
        Long empId = employee.getId();
        return Result.success(applyService.page(current, size, empId, null, null));
    }

    @ApiOperation("Submit an apply")
    @PostMapping
    public Result<Void> submit(@RequestBody AttendanceApply apply, HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims(request).get("userId").toString()));
        if (employee == null) {
            return Result.error("未绑定员工档案，无法提交考勤申请");
        }
        if (Integer.valueOf(1).equals(apply.getApplyType())) {
            if (apply.getSignType() == null || apply.getSignType() < 1 || apply.getSignType() > 3) {
                return Result.error("补签申请必须选择上午、下午或全天补签");
            }
            apply.setLeaveType(null);
            apply.setLeaveDays(null);
            apply.setOvertimeHours(null);
        } else if (Integer.valueOf(2).equals(apply.getApplyType())) {
            if (apply.getLeaveType() == null || (apply.getLeaveType() != 1 && apply.getLeaveType() != 2)) {
                return Result.error("请假申请必须选择事假或病假");
            }
            BigDecimal leaveDays = apply.getLeaveDays();
            if (leaveDays == null || leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("请填写有效的请假天数");
            }
            apply.setSignType(null);
            apply.setOvertimeHours(null);
        } else if (Integer.valueOf(3).equals(apply.getApplyType())) {
            BigDecimal overtimeHours = apply.getOvertimeHours();
            if (overtimeHours == null || overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("请填写有效的加班时长");
            }
            apply.setLeaveType(null);
            apply.setLeaveDays(null);
            apply.setSignType(null);
        } else {
            apply.setLeaveType(null);
            apply.setLeaveDays(null);
            apply.setSignType(null);
            apply.setOvertimeHours(null);
        }
        apply.setEmpId(employee.getId());
        apply.setEmpNo(employee.getEmpNo());
        apply.setEmpName(employee.getRealName());
        apply.setDeptId(employee.getDeptId());
        apply.setStatus(0); // 待审批
        applyService.save(apply);
        return Result.successMsg("Submitted");
    }

    @ApiOperation("Review an apply")
    @PutMapping("/{id}/review")
    public Result<Void> review(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String comment,
            HttpServletRequest request) {
        Claims claims = claims(request);
        Long managerId = Long.valueOf(claims.get("userId").toString());
        String reviewerName = claims.get("realName") != null
                ? claims.get("realName").toString()
                : (claims.get("username") != null ? claims.get("username").toString() : claims.getSubject());
        applyService.reviewApply(id, status, comment, managerId, reviewerName);
        return Result.successMsg("Reviewed");
    }

    @ApiOperation("Delete an apply")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AttendanceApply apply = applyService.getById(id);
        if (apply == null) {
            return Result.error("申请记录不存在");
        }
        applyService.removeById(id);
        Claims claims = claims(request);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "考勤申请",
                "删除考勤申请[" + applyTypeLabel(apply.getApplyType()) + "/" + (apply.getApplyDate() == null ? "-" : apply.getApplyDate()) + "]");
        return Result.successMsg("Deleted");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }

    private String applyTypeLabel(Integer applyType) {
        if (applyType == null) return "未知类型";
        switch (applyType) {
            case 1:
                return "补签";
            case 2:
                return "请假";
            case 3:
                return "加班申请";
            case 4:
                return "考勤异议";
            default:
                return "未知类型";
        }
    }
}
