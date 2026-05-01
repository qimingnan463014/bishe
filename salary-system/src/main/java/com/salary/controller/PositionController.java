package com.salary.controller;

import com.salary.common.Result;
import com.salary.dto.PositionBatchSaveRequest;
import com.salary.entity.Position;
import com.salary.service.PositionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "岗位管理")
@RestController
@RequestMapping("/position")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @ApiOperation("按部门查询岗位")
    @GetMapping("/list")
    public Result<List<Position>> listByDept(@RequestParam Long deptId) {
        return Result.success(positionService.listByDeptId(deptId));
    }

    @ApiOperation("按部门同步岗位")
    @PostMapping("/sync")
    public Result<Void> syncByDept(@RequestBody PositionBatchSaveRequest request) {
        positionService.syncByDept(request.getDeptId(), request.getPositions());
        return Result.successMsg("保存成功");
    }
}
