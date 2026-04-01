package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.AttendanceApply;
import com.salary.service.AttendanceApplyService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "Attendance Apply")
@RestController
@RequestMapping("/attendance-apply")
@RequiredArgsConstructor
public class AttendanceApplyController {

    private final AttendanceApplyService applyService;
    private final JwtUtil jwtUtil;

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
        return Result.success(applyService.page(current, size, empId, status, managerId));
    }

    @ApiOperation("Employee: my applies")
    @GetMapping("/my")
    public Result<PageResult<AttendanceApply>> myApplies(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long empId = Long.valueOf(claims(request).get("userId").toString());
        return Result.success(applyService.page(current, size, empId, null, null));
    }

    @ApiOperation("Submit an apply")
    @PostMapping
    public Result<Void> submit(@RequestBody AttendanceApply apply, HttpServletRequest request) {
        Long empId = Long.valueOf(claims(request).get("userId").toString());
        apply.setEmpId(empId);
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
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        applyService.reviewApply(id, status, comment, managerId);
        return Result.successMsg("Reviewed");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }
}
