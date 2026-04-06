package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.entity.SalaryFeedback;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.SalaryFeedbackService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api(tags = "Feedback & Complaint")
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class SalaryFeedbackController {

    private final SalaryFeedbackService feedbackService;
    private final EmployeeMapper employeeMapper;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list feedbacks (Admin sees all)")
    @GetMapping("/page")
    public Result<PageResult<SalaryFeedback>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Claims claims = claims(request);
        Integer role = claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString());
        Long empId = null;
        if (role != null && role != 1) {
            Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims.get("userId").toString()));
            empId = employee != null ? employee.getId() : null;
        }
        return Result.success(feedbackService.page(current, size, parseTypes(types), status, empId));
    }

    @ApiOperation("Employee: my feedbacks")
    @GetMapping("/my")
    public Result<PageResult<SalaryFeedback>> myFeedbacks(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String types,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims(request).get("userId").toString()));
        Long empId = employee != null ? employee.getId() : null;
        return Result.success(feedbackService.page(current, size, parseTypes(types), null, empId));
    }

    @ApiOperation("Salary feedback record center (Admin all / Manager self+subordinates / Employee self)")
    @GetMapping("/salary-page")
    public Result<PageResult<SalaryFeedback>> salaryPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Claims claims = claims(request);
        Integer role = claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString());
        if (role != null && role == 1) {
            return Result.success(feedbackService.page(current, size, Arrays.asList(1, 2), status, (List<Long>) null));
        }

        Long userId = Long.valueOf(claims.get("userId").toString());
        Employee self = employeeMapper.selectByUserId(userId);
        if (self == null) {
            return Result.success(new PageResult<>(current, size, 0, 0, Collections.emptyList()));
        }

        if (role != null && role == 2) {
            List<Long> empIds = new ArrayList<>();
            empIds.add(self.getId());
            employeeMapper.selectByManagerId(userId).stream()
                    .map(Employee::getId)
                    .filter(Objects::nonNull)
                    .filter(id -> !empIds.contains(id))
                    .forEach(empIds::add);
            return Result.success(feedbackService.page(current, size, Arrays.asList(1, 2), status, empIds));
        }

        return Result.success(feedbackService.page(current, size, Arrays.asList(1, 2), status, self.getId()));
    }

    @ApiOperation("Submit feedback/complaint")
    @PostMapping
    public Result<Void> submit(@RequestBody SalaryFeedback fb, HttpServletRequest request) {
        Claims claims = claims(request);
        Long userId = Long.valueOf(claims.get("userId").toString());
        Employee employee = employeeMapper.selectByUserId(userId);
        if (employee != null) {
            fb.setEmpId(employee.getId());
            fb.setEmpNo(employee.getEmpNo());
            fb.setEmpName(employee.getRealName());
        } else {
            fb.setEmpId(userId);
            fb.setEmpNo(claims.get("username") == null ? null : claims.get("username").toString());
            fb.setEmpName(claims.get("realName") == null ? claims.getSubject() : claims.get("realName").toString());
        }
        fb.setStatus(0); // 待处理
        feedbackService.save(fb);
        return Result.successMsg("Submitted");
    }

    @ApiOperation("Reply/Resolve feedback (Admin)")
    @PutMapping("/{id}/reply")
    public Result<Void> reply(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String replyContent,
            HttpServletRequest request) {
        Long replyUserId = Long.valueOf(claims(request).get("userId").toString());
        feedbackService.replyFeedback(id, status, replyContent, replyUserId);
        return Result.successMsg("Replied");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }

    private List<Integer> parseTypes(String types) {
        if (types == null || types.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.valueOf(s);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
