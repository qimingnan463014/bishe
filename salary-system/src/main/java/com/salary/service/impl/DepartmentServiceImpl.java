package com.salary.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.Department;
import com.salary.entity.Employee;
import com.salary.mapper.DepartmentMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门ServiceImpl实现类
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
        implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public PageResult<Department> page(int current, int size, String deptName, Integer status) {
        Page<Department> page = new Page<>(current, size);
        return PageResult.of(departmentMapper.selectPageWithCount(page, deptName, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDepartment(Department dept) {
        // 校验部门名称唯一性
        long count = lambdaQuery().eq(Department::getDeptName, dept.getDeptName()).count();
        if (count > 0) {
            throw new RuntimeException("部门名称已存在：" + dept.getDeptName());
        }
        // 兜底：自动生成部门编码（前缀DEPT + 6位随机大写字母数字）
        if (dept.getDeptCode() == null || dept.getDeptCode().isBlank()) {
            dept.setDeptCode("DEPT" + java.util.UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 6).toUpperCase());
        }
        // 兜底：基础工资默认为0
        if (dept.getBaseSalary() == null) {
            dept.setBaseSalary(java.math.BigDecimal.ZERO);
        }
        // 兜底：职位工资默认为0
        if (dept.getPositionSalary() == null) {
            dept.setPositionSalary(java.math.BigDecimal.ZERO);
        }
        save(dept);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(Department dept) {
        // 名称唯一性校验（排除自身）
        long count = lambdaQuery()
                .eq(Department::getDeptName, dept.getDeptName())
                .ne(Department::getId, dept.getId())
                .count();
        if (count > 0) {
            throw new RuntimeException("部门名称已存在：" + dept.getDeptName());
        }
        updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        // 检查是否有在职员工
        List<Employee> employees = employeeMapper.selectByDeptId(id);
        if (employees != null && !employees.isEmpty()) {
            throw new RuntimeException("该部门下存在在职员工，无法删除");
        }
        removeById(id);
    }

    @Override
    public List<Department> listAllEnabled() {
        return departmentMapper.selectAllEnabled();
    }

    @Override
    public Department getDetail(Long id) {
        return getById(id);
    }
}
