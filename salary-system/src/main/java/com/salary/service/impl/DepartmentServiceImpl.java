package com.salary.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.Department;
import com.salary.entity.Employee;
import com.salary.mapper.DepartmentMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.DepartmentService;
import com.salary.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
        implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionService positionService;

    @Override
    public PageResult<Department> page(int current, int size, String deptName, Integer status) {
        Page<Department> page = new Page<>(current, size);
        return PageResult.of(departmentMapper.selectPageWithCount(page, deptName, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDepartment(Department dept) {
        long count = lambdaQuery().eq(Department::getDeptName, dept.getDeptName()).count();
        if (count > 0) {
            throw new RuntimeException("部门名称已存在：" + dept.getDeptName());
        }
        if (!StringUtils.hasText(dept.getDeptCode())) {
            dept.setDeptCode("DEPT" + java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase());
        }
        if (dept.getBaseSalary() == null) {
            dept.setBaseSalary(BigDecimal.ZERO);
        }
        if (dept.getPositionSalary() == null) {
            dept.setPositionSalary(BigDecimal.ZERO);
        }
        save(dept);
        positionService.ensureManagerPosition(dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(Department dept) {
        Department before = getById(dept.getId());
        if (before == null) {
            throw new RuntimeException("部门不存在：" + dept.getId());
        }

        long count = lambdaQuery()
                .eq(Department::getDeptName, dept.getDeptName())
                .ne(Department::getId, dept.getId())
                .count();
        if (count > 0) {
            throw new RuntimeException("部门名称已存在：" + dept.getDeptName());
        }

        if (dept.getBaseSalary() == null) {
            dept.setBaseSalary(before.getBaseSalary());
        }
        if (dept.getPositionSalary() == null) {
            dept.setPositionSalary(before.getPositionSalary());
        }
        if (dept.getManagerId() == null) {
            dept.setManagerId(before.getManagerId());
        }

        updateById(dept);

        Department latest = getById(dept.getId());
        if (latest != null && shouldSyncEmployeeBaseSalary(before, latest)) {
            syncEmployeeBaseSalaryByDepartment(latest);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        List<Employee> employees = employeeMapper.selectByDeptId(id);
        if (employees != null && !employees.isEmpty()) {
            throw new RuntimeException("该部门下仍有在职员工，无法删除");
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

    private boolean shouldSyncEmployeeBaseSalary(Department before, Department latest) {
        return !Objects.equals(normalizeAmount(before.getBaseSalary()), normalizeAmount(latest.getBaseSalary()))
                || !Objects.equals(normalizeAmount(before.getPositionSalary()), normalizeAmount(latest.getPositionSalary()))
                || !Objects.equals(before.getManagerId(), latest.getManagerId());
    }

    /**
     * 事件触发式联动：
     * 仅回写员工档案表中的 baseSalary，不改历史薪资快照。
     */
    private void syncEmployeeBaseSalaryByDepartment(Department dept) {
        List<Employee> employees = employeeMapper.selectByDeptId(dept.getId());
        if (employees == null || employees.isEmpty()) {
            return;
        }
        BigDecimal deptBaseSalary = normalizeAmount(dept.getBaseSalary());
        BigDecimal deptPositionSalary = normalizeAmount(dept.getPositionSalary());
        for (Employee employee : employees) {
            BigDecimal targetSalary = deptBaseSalary;
            if (isManagerEmployee(employee, dept)) {
                targetSalary = targetSalary.add(deptPositionSalary);
            }
            targetSalary = targetSalary.setScale(2, RoundingMode.HALF_UP);
            if (employee.getBaseSalary() != null && employee.getBaseSalary().compareTo(targetSalary) == 0) {
                continue;
            }
            Employee patch = new Employee();
            patch.setId(employee.getId());
            patch.setBaseSalary(targetSalary);
            employeeMapper.updateById(patch);
        }
    }

    private boolean isManagerEmployee(Employee employee, Department dept) {
        if (employee == null) {
            return false;
        }
        if (employee.getRole() != null && employee.getRole() == 2) {
            return true;
        }
        if (employee.getUserRole() != null && employee.getUserRole() == 2) {
            return true;
        }
        if (StringUtils.hasText(employee.getPositionName()) && employee.getPositionName().contains("经理")) {
            return true;
        }
        return employee.getUserId() != null
                && dept.getManagerId() != null
                && dept.getManagerId().equals(employee.getUserId());
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
