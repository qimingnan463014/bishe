package com.salary.controller;

import com.salary.common.Result;
import com.salary.entity.Department;
import com.salary.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "Department Management")
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @ApiOperation("List all departments")
    @GetMapping("/list")
    public Result<List<Department>> list() {
        return Result.success(departmentService.list());
    }

    @ApiOperation("Add new department")
    @PostMapping
    public Result<Void> add(@RequestBody Department department) {
        departmentService.addDepartment(department);
        return Result.successMsg("Added successfully");
    }

    @ApiOperation("Update department")
    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        departmentService.updateDepartment(department);
        return Result.successMsg("Updated successfully");
    }

    @ApiOperation("Delete department")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.successMsg("Deleted successfully");
    }
}
