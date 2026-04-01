package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.SalaryPayment;
import com.salary.service.SalaryPaymentService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "Salary Payment")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class SalaryPaymentController {

    private final SalaryPaymentService paymentService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list payments")
    @GetMapping("/page")
    public Result<PageResult<SalaryPayment>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) Integer payStatus,
            HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Integer role = Integer.valueOf(c.get("role").toString());
        Long empId = role == 3 ? Long.valueOf(c.get("userId").toString()) : null;
        return Result.success(paymentService.page(current, size, yearMonth, empId, payStatus));
    }

    @ApiOperation("Execute payment (Admin only)")
    @PostMapping("/{id}/execute")
    public Result<Void> executePayment(@PathVariable Long id, HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Long operatorId = Long.valueOf(c.get("userId").toString());
        String operatorName = c.get("username").toString();
        paymentService.executePayment(id, operatorId, operatorName);
        return Result.successMsg("Payment executed successfully (Simulated)");
    }
}
