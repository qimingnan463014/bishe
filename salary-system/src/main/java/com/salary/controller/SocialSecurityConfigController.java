package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.dto.SocialSecurityAdjustRequest;
import com.salary.entity.SocialSecurityConfig;
import com.salary.service.SalaryService;
import com.salary.service.SocialSecurityConfigService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "Social Security Config")
@RestController
@RequestMapping("/social-config")
@RequiredArgsConstructor
public class SocialSecurityConfigController {

    private final SocialSecurityConfigService configService;
    private final SalaryService salaryService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;

    @ApiOperation("Page list configs")
    @GetMapping("/page")
    public Result<PageResult<SocialSecurityConfig>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String configName) {
        return Result.success(configService.page(current, size, configName));
    }

    @ApiOperation("Add config")
    @PostMapping
    public Result<Void> add(@RequestBody SocialSecurityConfig config) {
        configService.addConfig(config);
        return Result.successMsg("Added");
    }

    @ApiOperation("Update config")
    @PutMapping
    public Result<Void> update(@RequestBody SocialSecurityConfig config) {
        configService.updateConfig(config);
        return Result.successMsg("Updated");
    }

    @ApiOperation("Get current personal ratio config")
    @GetMapping("/current-personal")
    public Result<Map<String, Object>> getCurrentPersonalConfig() {
        return Result.success(toRatioResponse(configService.getCurrentPersonalConfig()));
    }

    @ApiOperation("Apply personal ratio config from effective month")
    @PostMapping("/apply-personal")
    public Result<Map<String, Object>> applyPersonalConfig(@RequestBody SocialSecurityAdjustRequest request,
                                                           HttpServletRequest httpRequest) {
        if (request == null || request.getEffectiveMonth() == null || request.getEffectiveMonth().trim().isEmpty()) {
            throw new RuntimeException("生效月份不能为空");
        }
        SocialSecurityConfig config = configService.applyPersonalConfig(
                request.getEffectiveMonth().trim(),
                request.getPensionRate(),
                request.getMedicalRate(),
                request.getUnemploymentRate(),
                request.getFundRate());
        salaryService.recalculateByEffectiveMonth(request.getEffectiveMonth().trim());

        Claims claims = claims(httpRequest);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "社保配置",
                "调整社保比例：养老" + formatPercent(config.getPensionEmpRatio())
                        + "%，医疗" + formatPercent(config.getMedicalEmpRatio())
                        + "%，失业" + formatPercent(config.getUnemploymentEmpRatio())
                        + "%，公积金" + formatPercent(config.getFundEmpRatio())
                        + "%，自" + request.getEffectiveMonth().trim() + "起生效");
        return Result.success(toRatioResponse(config));
    }

    @ApiOperation("Delete config")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        SocialSecurityConfig config = configService.getById(id);
        configService.removeById(id);
        Claims claims = claims(request);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "社保配置",
                "删除社保配置[" + (config != null ? config.getConfigName() : "ID=" + id) + "]");
        return Result.successMsg("Deleted");
    }

    private Claims claims(HttpServletRequest request) {
        return jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
    }

    private Map<String, Object> toRatioResponse(SocialSecurityConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configName", config.getConfigName());
        result.put("pensionRate", toPercent(config.getPensionEmpRatio()));
        result.put("medicalRate", toPercent(config.getMedicalEmpRatio()));
        result.put("unemploymentRate", toPercent(config.getUnemploymentEmpRatio()));
        result.put("fundRate", toPercent(config.getFundEmpRatio()));
        result.put("effectiveMonth", config.getEffectiveDate() == null ? null : config.getEffectiveDate().toString().substring(0, 7));
        result.put("baseMin", config.getCalcBaseMin());
        result.put("baseMax", config.getCalcBaseMax());
        return result;
    }

    private BigDecimal toPercent(BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO;
        }
        return ratio.multiply(new BigDecimal("100")).stripTrailingZeros();
    }

    private String formatPercent(BigDecimal ratio) {
        return toPercent(ratio).toPlainString();
    }
}
