package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.dto.SysLogCreateRequest;
import com.salary.entity.SysLog;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "System Log")
@RestController
@RequestMapping("/sys-log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page query system logs")
    @GetMapping("/page")
    public Result<PageResult<SysLog>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module) {
        return Result.success(sysLogService.page(current, size, username, module));
    }

    @ApiOperation("Create system log for frontend-local operations")
    @PostMapping("/manual")
    public Result<Void> manualCreate(@RequestBody SysLogCreateRequest request, HttpServletRequest httpServletRequest) {
        if (request == null || !StringUtils.hasText(request.getAction())) {
            return Result.error("日志内容不能为空");
        }
        Claims claims = claims(httpServletRequest);
        String username = claims.get("username") != null ? claims.get("username").toString() : claims.getSubject();
        Integer role = claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString());
        sysLogService.recordOperation(username, role, request.getModule(), request.getAction());
        return Result.successMsg("记录成功");
    }

    private Claims claims(HttpServletRequest request) {
        return jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
    }
}
