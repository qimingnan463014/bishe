package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.SalaryFeedback;
import com.salary.service.SalaryFeedbackService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "Feedback & Complaint")
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class SalaryFeedbackController {

    private final SalaryFeedbackService feedbackService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list feedbacks (Admin sees all)")
    @GetMapping("/page")
    public Result<PageResult<SalaryFeedback>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        return Result.success(feedbackService.page(current, size, type, status, null));
    }

    @ApiOperation("Employee: my feedbacks")
    @GetMapping("/my")
    public Result<PageResult<SalaryFeedback>> myFeedbacks(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long empId = Long.valueOf(claims(request).get("userId").toString());
        return Result.success(feedbackService.page(current, size, null, null, empId));
    }

    @ApiOperation("Submit feedback/complaint")
    @PostMapping
    public Result<Void> submit(@RequestBody SalaryFeedback fb, HttpServletRequest request) {
        Long empId = Long.valueOf(claims(request).get("userId").toString());
        fb.setEmpId(empId);
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
}
