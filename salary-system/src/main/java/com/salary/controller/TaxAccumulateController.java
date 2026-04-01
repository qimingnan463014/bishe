package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.TaxAccumulate;
import com.salary.service.TaxAccumulateService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "Tax Accumulation")
@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
public class TaxAccumulateController {

    private final TaxAccumulateService taxService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list tax records")
    @GetMapping("/page")
    public Result<PageResult<TaxAccumulate>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String taxYear,
            HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Integer role = Integer.valueOf(c.get("role").toString());
        Long empId = role == 3 ? Long.valueOf(c.get("userId").toString()) : null;
        
        return Result.success(taxService.page(current, size, taxYear, empId));
    }
}
