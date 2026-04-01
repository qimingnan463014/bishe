package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.SocialSecurityConfig;
import com.salary.service.SocialSecurityConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Social Security Config")
@RestController
@RequestMapping("/social-config")
@RequiredArgsConstructor
public class SocialSecurityConfigController {

    private final SocialSecurityConfigService configService;

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

    @ApiOperation("Delete config")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.removeById(id);
        return Result.successMsg("Deleted");
    }
}
