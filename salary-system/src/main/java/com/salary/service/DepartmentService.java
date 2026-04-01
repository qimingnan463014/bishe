package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.Department;

import java.util.List;

/**
 * 部门Service接口
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 分页查询部门列表
     *
     * @param current  当前页
     * @param size     每页数量
     * @param deptName 部门名称（模糊，可空）
     * @param status   状态（可空）
     */
    PageResult<Department> page(int current, int size, String deptName, Integer status);

    /**
     * 新增部门
     */
    void addDepartment(Department dept);

    /**
     * 修改部门（含工资基数）
     */
    void updateDepartment(Department dept);

    /**
     * 删除部门（逻辑删除前校验是否有在职员工）
     */
    void deleteDepartment(Long id);

    /**
     * 查询所有启用的部门（下拉框）
     */
    List<Department> listAllEnabled();

    /**
     * 根据ID查询部门详情
     */
    Department getDetail(Long id);
}
