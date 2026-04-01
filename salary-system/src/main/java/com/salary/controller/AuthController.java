package com.salary.controller;

import com.salary.common.Result;
import com.salary.service.UserService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Api(tags = "Authentication")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @ApiOperation("User Login")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @ApiParam("Username or Phone") @RequestParam String username,
            @ApiParam("Password") @RequestParam String password,
            @ApiParam("Captcha") @RequestParam(required = false) String captcha,
            @ApiParam("CaptchaKey") @RequestParam(required = false) String captchaKey) {
        return Result.success(userService.login(username, password, captcha, captchaKey));
    }

    @ApiOperation("Employee Login (C-side portal)")
    @PostMapping("/employee/login")
    public Result<Map<String, Object>> employeeLogin(
            @RequestParam String username,
            @RequestParam String password) {
        return Result.success(userService.employeeLogin(username, password));
    }

    @ApiOperation("Get current user info (personal center)")
    @GetMapping("/me")
    public Result<Object> me(HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Long userId = Long.valueOf(c.get("userId").toString());
        return Result.success(userService.getCurrentUserInfo(userId));
    }

    @ApiOperation("Change password")
    @PutMapping("/change-password")
    public Result<Void> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Long userId = Long.valueOf(c.get("userId").toString());
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.successMsg("密码修改成功");
    }

    @ApiOperation("Get Captcha")
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(userService.generateCaptcha());
    }
}

