package com.salary.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.dto.EmployeeImportExecuteResult;
import com.salary.dto.EmployeeImportDTO;
import com.salary.dto.EmployeeImportPreviewItem;
import com.salary.dto.EmployeeImportPreviewResult;
import com.salary.entity.Employee;
import com.salary.entity.Department;
import com.salary.entity.Position;
import com.salary.entity.User;
import com.salary.mapper.DepartmentMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.mapper.PositionMapper;
import com.salary.mapper.UserMapper;
import com.salary.service.EmployeeService;
import com.salary.service.PositionService;
import com.salary.util.ExcelAvatarExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PositionService   positionService;
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
        assignManagerPositionIfNeeded(employee);
        syncBaseSalaryWithDepartment(employee);
        save(employee);
        syncDepartmentManagerBinding(employee);
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
        assignManagerPositionIfNeeded(employee);
        syncBaseSalaryWithDepartment(employee);
        updateById(employee);
        syncDepartmentManagerBinding(employee);
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
        EmployeeImportPreviewResult preview = previewImport(file);
        List<EmployeeImportPreviewItem> items = preview.getItems() == null ? new ArrayList<>() : preview.getItems();
        items.forEach(item -> {
            if (Boolean.TRUE.equals(item.getExisting())) {
                item.setUpdateExisting(false);
            }
        });
        confirmImport(items);
    }

    @Override
    public EmployeeImportPreviewResult previewImport(MultipartFile file) {
        List<EmployeeImportDTO> dtoList = parseImportDtos(file);
        EmployeeImportPreviewResult result = new EmployeeImportPreviewResult();
        result.setTotalRows(dtoList.size());
        if (dtoList.isEmpty()) {
            return result;
        }

        Map<String, Long> deptNameToId = buildDeptNameMap();
        Map<Long, String> deptIdToName = buildDeptIdNameMap();
        Map<String, Long> deptPositionNameToId = buildDeptPositionNameMap();
        Map<Long, String> posIdToName = buildPositionIdNameMap();
        Set<String> managerEmpNosInFile = new HashSet<>();
        Set<String> empNos = new HashSet<>();
        for (EmployeeImportDTO dto : dtoList) {
            normalizeImportDto(dto);
            if (isManagerRole(dto.getRoleStr()) && StringUtils.hasText(dto.getEmpNo())) {
                managerEmpNosInFile.add(dto.getEmpNo());
            }
            if (StringUtils.hasText(dto.getEmpNo())) {
                empNos.add(dto.getEmpNo());
            }
        }

        Map<String, Employee> existingByEmpNo = loadExistingEmployeesByEmpNo(empNos);
        Map<Long, User> userById = loadRelatedUsers(existingByEmpNo.values());

        for (int index = 0; index < dtoList.size(); index++) {
            EmployeeImportDTO dto = dtoList.get(index);
            EmployeeImportPreviewItem item = buildPreviewItem(
                    dto,
                    index + 2,
                    deptNameToId,
                    deptPositionNameToId,
                    managerEmpNosInFile,
                    existingByEmpNo,
                    deptIdToName,
                    posIdToName,
                    userById
            );
            result.getItems().add(item);
            if (Boolean.TRUE.equals(item.getExisting())) {
                result.setExistingRows(result.getExistingRows() + 1);
            } else {
                result.setNewRows(result.getNewRows() + 1);
            }
            if (Boolean.TRUE.equals(item.getImportable())) {
                result.setImportableRows(result.getImportableRows() + 1);
            } else {
                result.setInvalidRows(result.getInvalidRows() + 1);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeImportExecuteResult confirmImport(List<EmployeeImportPreviewItem> items) {
        EmployeeImportExecuteResult result = new EmployeeImportExecuteResult();
        result.setTotalRows(items == null ? 0 : items.size());
        if (items == null || items.isEmpty()) {
            return result;
        }

        Map<String, Long> deptNameToId = buildDeptNameMap();
        Map<String, Long> deptPositionNameToId = buildDeptPositionNameMap();
        Set<String> empNos = new HashSet<>();
        for (EmployeeImportPreviewItem item : items) {
            normalizePreviewItem(item);
            if (StringUtils.hasText(item.getEmpNo())) {
                empNos.add(item.getEmpNo());
            }
        }

        Map<String, Employee> existingByEmpNo = loadExistingEmployeesByEmpNo(empNos);
        Map<String, Long> managerEmpNoToUserId = new HashMap<>();

        for (EmployeeImportPreviewItem item : items) {
            if (!canProcessImportItem(item, existingByEmpNo)) {
                continue;
            }
            if (isManagerRole(item.getRoleStr())) {
                Long userId = ensureUserForImport(item.getEmpNo(), item.getRealName(), 2);
                managerEmpNoToUserId.put(item.getEmpNo(), userId);
            }
        }

        for (EmployeeImportPreviewItem item : items) {
            if (!Boolean.TRUE.equals(item.getImportable()) || !StringUtils.hasText(item.getEmpNo())) {
                result.setSkippedRows(result.getSkippedRows() + 1);
                continue;
            }

            Employee existing = existingByEmpNo.get(item.getEmpNo());
            if (existing != null && !Boolean.TRUE.equals(item.getUpdateExisting())) {
                result.setSkippedRows(result.getSkippedRows() + 1);
                continue;
            }

            int role = isManagerRole(item.getRoleStr()) ? 2 : 3;
            Long userId = ensureUserForImport(item.getEmpNo(), item.getRealName(), role);
            if (existing == null) {
                Employee employee = new Employee();
                employee.setUserId(userId);
                applyImportFields(employee, item, deptNameToId, deptPositionNameToId, managerEmpNoToUserId);
                save(employee);
                syncDepartmentManagerBinding(employee);
                existingByEmpNo.put(employee.getEmpNo(), employee);
                result.setInsertedRows(result.getInsertedRows() + 1);
            } else {
                existing.setUserId(userId);
                applyImportFields(existing, item, deptNameToId, deptPositionNameToId, managerEmpNoToUserId);
                updateById(existing);
                syncDepartmentManagerBinding(existing);
                existingByEmpNo.put(existing.getEmpNo(), existing);
                result.setUpdatedRows(result.getUpdatedRows() + 1);
            }
        }
        log.info("员工导入确认完成：新增{}条，更新{}条，跳过{}条",
                result.getInsertedRows(), result.getUpdatedRows(), result.getSkippedRows());
        return result;
    }

    // ====================================================
    //  EasyExcel 导出
    // ====================================================

    @Override
    public void exportToExcel(HttpServletResponse response,
                               String empNo, String realName, Long deptId) {
        Page<Employee> page = new Page<>(1, 50000);
        List<Employee> list = employeeMapper.selectPageWithDetails(page, empNo, realName, deptId, 1, null).getRecords();
        ExcelAvatarExportUtil.export(
                response,
                "员工信息.xlsx",
                "员工信息",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("工号", 12, Employee::getEmpNo),
                        ExcelAvatarExportUtil.text("密码", 12, row -> ExcelAvatarExportUtil.firstNonBlank(row.getLoginPassword(), row.getInitialPassword(), "123456")),
                        ExcelAvatarExportUtil.text("姓名", 12, Employee::getRealName),
                        ExcelAvatarExportUtil.text("性别", 10, row -> row.getGender() != null && row.getGender() == 2 ? "女" : "男"),
                        ExcelAvatarExportUtil.text("手机", 14, Employee::getPhone),
                        ExcelAvatarExportUtil.text("身份证号", 22, Employee::getIdCard),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("岗位", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getPositionName(), "-")),
                        ExcelAvatarExportUtil.text("银行卡号", 22, row -> ExcelAvatarExportUtil.firstNonBlank(row.getBankAccount(), row.getBankCard(), "-")),
                        ExcelAvatarExportUtil.text("开户行", 18, row -> ExcelAvatarExportUtil.firstNonBlank(row.getBankName(), "-")),
                        ExcelAvatarExportUtil.text("经理账号", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerNo(), "-")),
                        ExcelAvatarExportUtil.text("部门经理", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), "-")),
                        ExcelAvatarExportUtil.text("基本工资", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getBaseSalary())),
                        ExcelAvatarExportUtil.avatar("头像", 12, Employee::getAvatar),
                        ExcelAvatarExportUtil.text("入职日期", 14, row -> ExcelAvatarExportUtil.formatDate(row.getHireDate())),
                        ExcelAvatarExportUtil.text("状态", 12, row -> {
                            Integer status = row.getStatus();
                            if (status == null) {
                                return "-";
                            }
                            if (status == 1) {
                                return "在职";
                            }
                            if (status == 2) {
                                return "离职";
                            }
                            if (status == 3) {
                                return "试用期";
                            }
                            return "未知";
                        })
                ),
                list
        );
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
        User u = new User();
        u.setUsername(empNo);
        u.setPassword(encoder.encode("123456"));
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

    private Map<String, Long> buildDeptPositionNameMap() {
        Map<String, Long> map = new HashMap<>();
        positionMapper.selectList(null).forEach(p -> {
            if (p.getDeptId() != null && StringUtils.hasText(p.getPositionName())) {
                map.put(buildDeptPositionKey(p.getDeptId(), p.getPositionName()), p.getId());
            }
        });
        return map;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), DATE_FMT); } catch (Exception e) { return null; }
    }

    private void bindManager(Employee employee,
                             String managerNo,
                             Map<String, Long> managerEmpNoToUserId) {
        if (employee == null) {
            return;
        }
        if (StringUtils.hasText(managerNo)) {
            String normalizedManagerNo = managerNo.trim();
            Long managerUserId = managerEmpNoToUserId.get(normalizedManagerNo);
            if (managerUserId != null) {
                employee.setManagerId(managerUserId);
                return;
            }
            User existingManager = userMapper.selectByUsernameWithPassword(normalizedManagerNo);
            if (existingManager != null && existingManager.getRole() != null && existingManager.getRole() == 2) {
                employee.setManagerId(existingManager.getId());
                return;
            }
        }

        if (employee.getDeptId() == null) {
            return;
        }
        Department department = deptMapper.selectById(employee.getDeptId());
        if (department != null && department.getManagerId() != null) {
            employee.setManagerId(department.getManagerId());
        }
    }

    private List<EmployeeImportDTO> parseImportDtos(MultipartFile file) {
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
        return dtoList;
    }

    private EmployeeImportPreviewItem buildPreviewItem(EmployeeImportDTO dto,
                                                       int rowNo,
                                                       Map<String, Long> deptNameToId,
                                                       Map<String, Long> deptPositionNameToId,
                                                       Set<String> managerEmpNosInFile,
                                                       Map<String, Employee> existingByEmpNo,
                                                       Map<Long, String> deptIdToName,
                                                       Map<Long, String> posIdToName,
                                                       Map<Long, User> userById) {
        EmployeeImportPreviewItem item = new EmployeeImportPreviewItem();
        item.setRowNo(rowNo);
        item.setEmpNo(dto.getEmpNo());
        item.setRealName(dto.getRealName());
        item.setGenderStr(dto.getGenderStr());
        item.setPhone(dto.getPhone());
        item.setIdCard(dto.getIdCard());
        item.setDeptName(dto.getDeptName());
        item.setPositionName(dto.getPositionName());
        item.setHireDate(dto.getHireDate());
        item.setBankAccount(dto.getBankAccount());
        item.setBankName(dto.getBankName());
        item.setRoleStr(normalizeRoleStr(dto.getRoleStr()));
        item.setManagerNo(dto.getManagerNo());

        List<String> problems = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        boolean managerRole = isManagerRole(dto.getRoleStr());
        Long deptId = StringUtils.hasText(dto.getDeptName()) ? deptNameToId.get(dto.getDeptName()) : null;
        if (!StringUtils.hasText(dto.getEmpNo())) {
            problems.add("工号不能为空");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            problems.add("姓名不能为空");
        }
        if (deptId == null) {
            problems.add("部门不存在");
        }
        if (managerRole) {
            notes.add("经理岗位将按所属部门自动绑定");
        } else if (!StringUtils.hasText(dto.getPositionName())) {
            problems.add("岗位不能为空");
        } else if (deptId == null || !deptPositionNameToId.containsKey(buildDeptPositionKey(deptId, dto.getPositionName()))) {
            problems.add("岗位不存在或不属于该部门");
        }
        if (StringUtils.hasText(dto.getManagerNo())
                && !managerEmpNosInFile.contains(dto.getManagerNo())) {
            User manager = userMapper.selectByUsernameWithPassword(dto.getManagerNo());
            if (manager == null || manager.getRole() == null || manager.getRole() != 2) {
                notes.add("经理工号未匹配，将按部门经理兜底");
            }
        }

        Employee existing = existingByEmpNo.get(dto.getEmpNo());
        item.setExisting(existing != null);
        item.setUpdateExisting(false);
        item.setImportable(problems.isEmpty());
        item.setMessage(problems.isEmpty() ? String.join("；", notes) : String.join("；", problems));
        if (existing != null) {
            item.setCurrentSummary(buildCurrentSummary(existing, deptIdToName, posIdToName, userById));
        } else {
            item.setCurrentSummary("-");
        }
        return item;
    }

    private Map<String, Employee> loadExistingEmployeesByEmpNo(Set<String> empNos) {
        if (empNos == null || empNos.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Employee> existingList = lambdaQuery().in(Employee::getEmpNo, empNos).list();
        Map<String, Employee> result = new HashMap<>();
        for (Employee employee : existingList) {
            result.put(employee.getEmpNo(), employee);
        }
        return result;
    }

    private Map<Long, User> loadRelatedUsers(Collection<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = new HashSet<>();
        for (Employee employee : employees) {
            if (employee.getUserId() != null) {
                userIds.add(employee.getUserId());
            }
            if (employee.getManagerId() != null) {
                userIds.add(employee.getManagerId());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, User> userById = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            userById.put(user.getId(), user);
        }
        return userById;
    }

    private String buildCurrentSummary(Employee existing,
                                       Map<Long, String> deptIdToName,
                                       Map<Long, String> posIdToName,
                                       Map<Long, User> userById) {
        User user = existing.getUserId() == null ? null : userById.get(existing.getUserId());
        User manager = existing.getManagerId() == null ? null : userById.get(existing.getManagerId());
        String roleText = user != null && user.getRole() != null && user.getRole() == 2 ? "经理" : "员工";
        String deptName = deptIdToName.getOrDefault(existing.getDeptId(), "-");
        String positionName = posIdToName.getOrDefault(existing.getPositionId(), StringUtils.hasText(existing.getPositionName()) ? existing.getPositionName() : "-");
        String managerNo = manager != null ? manager.getUsername() : "-";
        return "姓名:" + safeText(existing.getRealName())
                + " / 部门:" + safeText(deptName)
                + " / 岗位:" + safeText(positionName)
                + " / 角色:" + roleText
                + " / 经理:" + safeText(managerNo);
    }

    private void normalizeImportDto(EmployeeImportDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setEmpNo(normalizeText(dto.getEmpNo()));
        dto.setRealName(normalizeText(dto.getRealName()));
        dto.setGenderStr(normalizeText(dto.getGenderStr()));
        dto.setPhone(normalizeText(dto.getPhone()));
        dto.setIdCard(normalizeText(dto.getIdCard()));
        dto.setDeptName(normalizeText(dto.getDeptName()));
        dto.setPositionName(normalizeText(dto.getPositionName()));
        dto.setHireDate(normalizeText(dto.getHireDate()));
        dto.setBankAccount(normalizeText(dto.getBankAccount()));
        dto.setBankName(normalizeText(dto.getBankName()));
        dto.setRoleStr(normalizeRoleStr(dto.getRoleStr()));
        dto.setManagerNo(normalizeText(dto.getManagerNo()));
    }

    private void normalizePreviewItem(EmployeeImportPreviewItem item) {
        if (item == null) {
            return;
        }
        item.setEmpNo(normalizeText(item.getEmpNo()));
        item.setRealName(normalizeText(item.getRealName()));
        item.setGenderStr(normalizeText(item.getGenderStr()));
        item.setPhone(normalizeText(item.getPhone()));
        item.setIdCard(normalizeText(item.getIdCard()));
        item.setDeptName(normalizeText(item.getDeptName()));
        item.setPositionName(normalizeText(item.getPositionName()));
        item.setHireDate(normalizeText(item.getHireDate()));
        item.setBankAccount(normalizeText(item.getBankAccount()));
        item.setBankName(normalizeText(item.getBankName()));
        item.setRoleStr(normalizeRoleStr(item.getRoleStr()));
        item.setManagerNo(normalizeText(item.getManagerNo()));
    }

    private boolean canProcessImportItem(EmployeeImportPreviewItem item, Map<String, Employee> existingByEmpNo) {
        if (!Boolean.TRUE.equals(item.getImportable()) || !StringUtils.hasText(item.getEmpNo())) {
            return false;
        }
        Employee existing = existingByEmpNo.get(item.getEmpNo());
        return existing == null || Boolean.TRUE.equals(item.getUpdateExisting());
    }

    private Long ensureUserForImport(String empNo, String realName, int role) {
        User exist = userMapper.selectByUsernameWithPassword(empNo);
        if (exist != null) {
            boolean changed = false;
            if (!Objects.equals(exist.getRealName(), realName)) {
                exist.setRealName(realName);
                changed = true;
            }
            if (!Objects.equals(exist.getRole(), role)) {
                exist.setRole(role);
                changed = true;
            }
            if (!Objects.equals(exist.getStatus(), 1)) {
                exist.setStatus(1);
                changed = true;
            }
            if (changed) {
                userMapper.updateById(exist);
            }
            return exist.getId();
        }
        return createUserIfAbsent(empNo, realName, null, role, passwordEncoder);
    }

    private void applyImportFields(Employee employee,
                                   EmployeeImportPreviewItem item,
                                   Map<String, Long> deptNameToId,
                                   Map<String, Long> deptPositionNameToId,
                                   Map<String, Long> managerEmpNoToUserId) {
        int role = isManagerRole(item.getRoleStr()) ? 2 : 3;
        employee.setEmpNo(item.getEmpNo());
        employee.setRealName(item.getRealName());
        employee.setGender("女".equals(item.getGenderStr()) ? 2 : 1);
        employee.setPhone(item.getPhone());
        employee.setIdCard(item.getIdCard());
        employee.setDeptId(deptNameToId.get(item.getDeptName()));
        employee.setHireDate(parseDate(item.getHireDate()));
        employee.setRole(role);
        if (role == 2) {
            employee.setManagerId(null);
            assignManagerPositionIfNeeded(employee);
        } else {
            Long positionId = resolveDeptPositionId(employee.getDeptId(), item.getPositionName(), deptPositionNameToId);
            if (positionId == null) {
                throw new RuntimeException("岗位不存在或不属于该部门：" + safeText(item.getPositionName()));
            }
            employee.setPositionId(positionId);
            employee.setPositionName(item.getPositionName());
            bindManager(employee, item.getManagerNo(), managerEmpNoToUserId);
        }
        employee.setBankAccount(item.getBankAccount());
        employee.setBankName(item.getBankName());
        employee.setStatus(1);
        syncBaseSalaryWithDepartment(employee);
    }

    private void assignManagerPositionIfNeeded(Employee employee) {
        if (!isManagerEmployee(employee, null) || employee.getDeptId() == null) {
            return;
        }
        Position managerPosition = positionService.ensureManagerPosition(employee.getDeptId());
        employee.setPositionId(managerPosition.getId());
        employee.setPositionName(managerPosition.getPositionName());
    }

    private void syncDepartmentManagerBinding(Employee employee) {
        if (!isManagerEmployee(employee, null) || employee.getDeptId() == null || employee.getUserId() == null) {
            return;
        }
        Department dept = deptMapper.selectById(employee.getDeptId());
        if (dept == null || Objects.equals(dept.getManagerId(), employee.getUserId())) {
            return;
        }
        dept.setManagerId(employee.getUserId());
        deptMapper.updateById(dept);
    }

    private Long resolveDeptPositionId(Long deptId, String positionName, Map<String, Long> deptPositionNameToId) {
        if (deptId == null || !StringUtils.hasText(positionName) || deptPositionNameToId == null) {
            return null;
        }
        return deptPositionNameToId.get(buildDeptPositionKey(deptId, positionName));
    }

    private String buildDeptPositionKey(Long deptId, String positionName) {
        return deptId + "#" + normalizeText(positionName);
    }

    private Map<Long, String> buildDeptIdNameMap() {
        Map<Long, String> map = new HashMap<>();
        deptMapper.selectAllEnabled().forEach(d -> map.put(d.getId(), d.getDeptName()));
        return map;
    }

    private Map<Long, String> buildPositionIdNameMap() {
        Map<Long, String> map = new HashMap<>();
        positionMapper.selectList(null).forEach(p -> map.put(p.getId(), p.getPositionName()));
        return map;
    }

    private boolean isManagerRole(String roleStr) {
        return "经理".equals(normalizeRoleStr(roleStr));
    }

    private String normalizeRoleStr(String roleStr) {
        return "经理".equals(normalizeText(roleStr)) ? "经理" : "员工";
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeText(String text) {
        return StringUtils.hasText(text) ? text : "-";
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
