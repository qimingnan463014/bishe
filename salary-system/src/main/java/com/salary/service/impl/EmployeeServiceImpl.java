package com.salary.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.dto.EmployeeImportDTO;
import com.salary.entity.Employee;
import com.salary.entity.Department;
import com.salary.entity.User;
import com.salary.mapper.DepartmentMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.mapper.PositionMapper;
import com.salary.mapper.UserMapper;
import com.salary.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 员工 ServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmployeeMapper    employeeMapper;
    private final UserMapper        userMapper;
    private final DepartmentMapper  deptMapper;
    private final PositionMapper    positionMapper;
    private final PasswordEncoder   passwordEncoder;

    // ====================================================
    //  查询
    // ====================================================

    @Override
    public PageResult<Employee> page(int current, int size,
                                      String empNo, String realName,
                                      Long deptId, Integer status, Long managerId) {
        Page<Employee> page = new Page<>(current, size);
        return PageResult.of(employeeMapper.selectPageWithDetails(
                page, empNo, realName, deptId, status, managerId));
    }

    @Override
    public Employee getDetail(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public Employee getByCurrentUser(Long userId) {
        return employeeMapper.selectByUserId(userId);
    }

    // ====================================================
    //  新增员工（同步创建登录账号）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEmployee(Employee employee, String initialPassword) {
        // 工号唯一性校验
        long empCount = lambdaQuery().eq(Employee::getEmpNo, employee.getEmpNo()).count();
        if (empCount > 0) throw new RuntimeException("工号已存在：" + employee.getEmpNo());

        int targetRole = isManagerEmployee(employee, null) ? 2 : 3;

        // 1. 创建系统账户（username = empNo）
        User user = new User();
        user.setUsername(employee.getEmpNo());
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRealName(employee.getRealName());
        user.setRole(targetRole);
        user.setStatus(1);
        userMapper.insert(user);

        // 2. 绑定 userId 保存员工档案
        employee.setUserId(user.getId());
        syncBaseSalaryWithDepartment(employee);
        save(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(Employee employee) {
        Employee exist = getById(employee.getId());
        if (exist == null) {
            throw new RuntimeException("员工不存在：" + employee.getId());
        }
        if (employee.getUserId() == null) {
            employee.setUserId(exist.getUserId());
        }
        if (employee.getDeptId() == null) {
            employee.setDeptId(exist.getDeptId());
        }
        syncBaseSalaryWithDepartment(employee);
        updateById(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        Employee emp = getById(id);
        if (emp == null) return;
        // 1. 物理删除员工档案
        removeById(id);
        // 2. 物理删除关联的登录账号
        if (emp.getUserId() != null) {
            userMapper.deleteById(emp.getUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ids.forEach(this::deleteEmployee);
    }

    // ====================================================
    //  ★ EasyExcel 批量导入员工
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importByExcel(MultipartFile file) {
        List<EmployeeImportDTO> dtoList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), EmployeeImportDTO.class,
                    new ReadListener<EmployeeImportDTO>() {
                        @Override
                        public void invoke(EmployeeImportDTO data, AnalysisContext ctx) {
                            dtoList.add(data);
                        }
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {
                            log.info("员工Excel解析完毕，共{}行", dtoList.size());
                        }
                    }).sheet().doRead();
        } catch (Exception e) {
            throw new RuntimeException("员工Excel解析失败：" + e.getMessage(), e);
        }

        // 预加载部门、岗位 Map，避免循环查库
        Map<String, Long> deptNameToId  = buildDeptNameMap();
        Map<String, Long> posNameToId   = buildPositionNameMap();
        Map<String, Long> empNoToUserId = new HashMap<>();

        // 第一遍：经理账号先创建，员工绑定 managerId 需要
        Map<String, Long> managerEmpNoToUserId = new HashMap<>();
        for (EmployeeImportDTO dto : dtoList) {
            if ("经理".equals(dto.getRoleStr())) {
                Long userId = createUserIfAbsent(dto.getEmpNo(), dto.getRealName(),
                        dto.getPhone(), 2, passwordEncoder); // role=2 经理
                managerEmpNoToUserId.put(dto.getEmpNo(), userId);
            }
        }

        // 第二遍：创建全部员工记录
        List<Employee> toSave = new ArrayList<>();
        for (EmployeeImportDTO dto : dtoList) {
            try {
                // 已存在的工号跳过
                long cnt = lambdaQuery().eq(Employee::getEmpNo, dto.getEmpNo()).count();
                if (cnt > 0) {
                    log.warn("工号已存在，跳过导入：{}", dto.getEmpNo());
                    continue;
                }

                // 确定角色与账号
                int role = "经理".equals(dto.getRoleStr()) ? 2 : 3;
                Long userId;
                if (role == 2) {
                    userId = managerEmpNoToUserId.get(dto.getEmpNo());
                } else {
                    userId = createUserIfAbsent(dto.getEmpNo(), dto.getRealName(),
                            dto.getPhone(), 3, passwordEncoder);
                }
                empNoToUserId.put(dto.getEmpNo(), userId);

                // 构建员工档案
                Employee emp = new Employee();
                emp.setUserId(userId);
                emp.setEmpNo(dto.getEmpNo());
                emp.setRealName(dto.getRealName());
                emp.setGender("女".equals(dto.getGenderStr()) ? 2 : 1);
                emp.setPhone(dto.getPhone());
                emp.setIdCard(dto.getIdCard());
                emp.setDeptId(deptNameToId.get(dto.getDeptName()));
                emp.setPositionId(posNameToId.get(dto.getPositionName()));
                emp.setHireDate(parseDate(dto.getHireDate()));
                emp.setRole(role);
                emp.setPositionName(dto.getPositionName());
                syncBaseSalaryWithDepartment(emp);
                emp.setBankAccount(dto.getBankAccount());
                emp.setBankName(dto.getBankName());
                emp.setStatus(1); // 在职
                // 绑定经理（若填了经理工号）
                if (dto.getManagerNo() != null && managerEmpNoToUserId.containsKey(dto.getManagerNo())) {
                    emp.setManagerId(managerEmpNoToUserId.get(dto.getManagerNo()));
                }
                toSave.add(emp);
            } catch (Exception ex) {
                log.error("员工 {} 导入失败：{}", dto.getEmpNo(), ex.getMessage());
            }
        }

        if (!toSave.isEmpty()) {
            saveBatch(toSave, 100); // MyBatis-Plus 批量插入
            log.info("员工Excel导入完成，成功{}条", toSave.size());
        }
    }

    // ====================================================
    //  EasyExcel 导出
    // ====================================================

    @Override
    public void exportToExcel(HttpServletResponse response,
                               String empNo, String realName, Long deptId) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try {
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("员工信息.xlsx", "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<Employee> list = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(StringUtils.hasText(empNo), Employee::getEmpNo, empNo)
                        .like(StringUtils.hasText(realName), Employee::getRealName, realName)
                        .eq(deptId != null, Employee::getDeptId, deptId)
                        .eq(Employee::getStatus, 1));
        try {
            EasyExcel.write(response.getOutputStream(), Employee.class)
                    .sheet("员工信息").doWrite(list);
        } catch (IOException e) {
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    // ====================================================
    //  工具方法
    // ====================================================

    /** 若账号不存在则创建，返回 user.id */
    private Long createUserIfAbsent(String empNo, String realName,
                                     String phone, int role,
                                     PasswordEncoder encoder) {
        User exist = userMapper.selectByUsernameWithPassword(empNo);
        if (exist != null) return exist.getId();
        String initPwd = (phone != null && phone.length() >= 6)
                ? phone.substring(phone.length() - 6) : "123456";
        User u = new User();
        u.setUsername(empNo);
        u.setPassword(encoder.encode(initPwd));
        u.setRealName(realName);
        u.setRole(role);
        u.setStatus(1);
        userMapper.insert(u);
        return u.getId();
    }

    private void syncBaseSalaryWithDepartment(Employee employee) {
        if (employee == null || employee.getDeptId() == null) {
            return;
        }
        Department dept = deptMapper.selectById(employee.getDeptId());
        if (dept == null) {
            return;
        }
        BigDecimal baseSalary = dept.getBaseSalary() != null ? dept.getBaseSalary() : BigDecimal.ZERO;
        if (isManagerEmployee(employee, dept)) {
            baseSalary = baseSalary.add(dept.getPositionSalary() != null ? dept.getPositionSalary() : BigDecimal.ZERO);
        }
        employee.setBaseSalary(baseSalary.setScale(2, RoundingMode.HALF_UP));
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
                && dept != null
                && dept.getManagerId() != null
                && dept.getManagerId().equals(employee.getUserId());
    }

    private Map<String, Long> buildDeptNameMap() {
        Map<String, Long> map = new HashMap<>();
        deptMapper.selectAllEnabled().forEach(d -> map.put(d.getDeptName(), d.getId()));
        return map;
    }

    private Map<String, Long> buildPositionNameMap() {
        Map<String, Long> map = new HashMap<>();
        positionMapper.selectList(null).forEach(p -> map.put(p.getPositionName(), p.getId()));
        return map;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), DATE_FMT); } catch (Exception e) { return null; }
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    @Override
    public List<java.util.Map<String, Object>> getDeptDistribution() {
        return employeeMapper.countByDepartment();
    }

    @Override
    public PageResult<Employee> pageManager(int current, int size, String empNo, String realName,
                                            Long deptId, Integer status) {
        Page<Employee> page = new Page<>(current, size);
        return PageResult.of(employeeMapper.selectPageWithDetailsForManager(page, empNo, realName, deptId, status));
    }
}
