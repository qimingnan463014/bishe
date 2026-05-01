package com.salary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.entity.SalaryRecord;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.SalaryService;
import com.salary.util.ExcelAvatarExportUtil;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Salary Controller
 *
 * Status flow: Draft(1) -> Published(2) -> Audited(3) -> Paid(4)
 * Role rules:
 *   Admin(1)   - full access, only one who can call pay
 *   Manager(2) - query own dept, publish salary
 *   Employee(3)- query own published salary slip (status >= 2)
 */
@Api(tags = "Salary Management")
@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
@Slf4j
public class SalaryController {

    private static final Path ISSUE_FILE_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "salary-issue")
            .toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_ISSUE_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "zip"
    ));

    private final SalaryService salaryService;
    private final JwtUtil jwtUtil;
    private final EmployeeMapper employeeMapper;

    @ApiOperation("Page query salary records")
    @GetMapping("/page")
    public Result<PageResult<SalaryRecord>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer calcStatus,
            HttpServletRequest request) {
        Integer role = getRole(request);
        Long managerId = role == 2 ? getUserId(request) : null;
        String excludeEmpNo = null;
        Boolean excludeDraft = role == 1;
        return Result.success(salaryService.page(
                current, size, yearMonth, empNo, realName, deptId, calcStatus, managerId, excludeEmpNo, excludeDraft));
    }

    @ApiOperation("导出个税与社保 Excel")
    @GetMapping("/tax-export")
    public void exportTax(HttpServletResponse response,
                          @RequestParam(required = false) String realName,
                          @RequestParam(required = false) Long deptId,
                          HttpServletRequest request) {
        RoleScope scope = resolveRoleScope(request);
        List<SalaryRecord> records = salaryService.page(
                1, 50000, null, null, realName, deptId, null,
                scope.managerId, scope.excludeEmpNo, scope.excludeDraft).getRecords();
        EmployeeLookup lookup = loadEmployeeLookup();
        records.removeIf(row -> !matchKeyword(resolveEmployeeName(row, lookup), realName));
        ExcelAvatarExportUtil.export(
                response,
                "工资个税与社保.xlsx",
                "工资个税与社保",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("员工姓名", 12, row -> resolveEmployeeName(row, lookup)),
                        ExcelAvatarExportUtil.avatar("头像", 12, row -> resolveEmployee(row, lookup).getAvatar()),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getDeptName(), resolveEmployee(row, lookup).getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("薪资基数", 14, row -> ExcelAvatarExportUtil.formatNumber(resolveDisplayBaseSalary(row, lookup))),
                        ExcelAvatarExportUtil.text("公积金", 12, row -> ExcelAvatarExportUtil.formatNumber(calcByRatio(resolveCalcBase(row, lookup), "0.12"))),
                        ExcelAvatarExportUtil.text("医疗保险", 12, row -> ExcelAvatarExportUtil.formatNumber(calcByRatio(resolveCalcBase(row, lookup), "0.02"))),
                        ExcelAvatarExportUtil.text("失业保险", 12, row -> ExcelAvatarExportUtil.formatNumber(calcByRatio(resolveCalcBase(row, lookup), "0.003"))),
                        ExcelAvatarExportUtil.text("养老保险", 12, row -> ExcelAvatarExportUtil.formatNumber(calcByRatio(resolveCalcBase(row, lookup), "0.08"))),
                        ExcelAvatarExportUtil.text("总社保扣款", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getSocialSecurityEmp())),
                        ExcelAvatarExportUtil.text("个税扣缴", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getIncomeTax())),
                        ExcelAvatarExportUtil.text("扣除日期", 14, row -> ExcelAvatarExportUtil.formatDate(firstNonNull(row.getRecordDate(), row.getPayDate(), row.getCreateTime()))),
                        ExcelAvatarExportUtil.text("经理账号", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerNo(), resolveEmployee(row, lookup).getManagerNo(), "-")),
                        ExcelAvatarExportUtil.text("部门经理", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), resolveEmployee(row, lookup).getManagerName(), "-"))
                ),
                records
        );
    }

    @ApiOperation("导出薪资核算 Excel")
    @GetMapping("/export")
    public void exportSalary(HttpServletResponse response,
                             @RequestParam(required = false) String yearMonth,
                             @RequestParam(required = false) String empNo,
                             @RequestParam(required = false) String realName,
                             @RequestParam(required = false) Long deptId,
                             @RequestParam(required = false) Integer calcStatus,
                             @RequestParam(required = false) String managerName,
                             HttpServletRequest request) {
        RoleScope scope = resolveRoleScope(request);
        List<SalaryRecord> records = salaryService.page(
                1, 50000, yearMonth, empNo, realName, deptId, calcStatus,
                scope.managerId, scope.excludeEmpNo, scope.excludeDraft).getRecords();
        EmployeeLookup lookup = loadEmployeeLookup();
        records.removeIf(row -> !matchKeyword(
                ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), resolveEmployee(row, lookup).getManagerName(), "-"),
                managerName));
        ExcelAvatarExportUtil.export(
                response,
                "薪资核算.xlsx",
                "薪资核算",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("月份", 12, SalaryRecord::getYearMonth),
                        ExcelAvatarExportUtil.text("工号", 12, row -> ExcelAvatarExportUtil.firstNonBlank(row.getEmpNo(), resolveEmployee(row, lookup).getEmpNo(), "-")),
                        ExcelAvatarExportUtil.text("姓名", 12, row -> resolveEmployeeName(row, lookup)),
                        ExcelAvatarExportUtil.avatar("头像", 12, row -> resolveEmployee(row, lookup).getAvatar()),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getDeptName(), resolveEmployee(row, lookup).getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("银行卡号", 22, row -> ExcelAvatarExportUtil.firstNonBlank(row.getBankAccount(), resolveEmployee(row, lookup).getBankAccount(), "-")),
                        ExcelAvatarExportUtil.text("基本工资", 14, row -> ExcelAvatarExportUtil.formatNumber(resolveDisplayBaseSalary(row, lookup))),
                        ExcelAvatarExportUtil.text("加班工资", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getOvertimePay())),
                        ExcelAvatarExportUtil.text("绩效奖金", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getPerfBonus())),
                        ExcelAvatarExportUtil.text("津贴", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getAllowance())),
                        ExcelAvatarExportUtil.text("五险一金", 14, row -> formatNegativeNumber(row.getSocialSecurityEmp())),
                        ExcelAvatarExportUtil.text("个税", 12, row -> formatNegativeNumber(row.getIncomeTax())),
                        ExcelAvatarExportUtil.text("扣款金额", 14, row -> formatNegativeNumber(resolveDeductAmount(row))),
                        ExcelAvatarExportUtil.text("实发工资", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getNetSalary())),
                        ExcelAvatarExportUtil.text("状态", 12, row -> salaryStatusText(row.getCalcStatus()))
                ),
                records
        );
    }

    @ApiOperation("导出薪资发放 Excel")
    @GetMapping("/payment-export")
    public void exportPayment(HttpServletResponse response,
                              @RequestParam(required = false) String yearMonth,
                              @RequestParam(required = false) String empNo,
                              @RequestParam(required = false) String realName,
                              @RequestParam(required = false) Long deptId,
                              @RequestParam(required = false) Integer payStatus,
                              HttpServletRequest request) {
        RoleScope scope = resolveRoleScope(request);
        List<SalaryRecord> records = salaryService.page(
                1, 50000, yearMonth, empNo, realName, deptId, payStatus,
                scope.managerId, scope.excludeEmpNo, scope.excludeDraft).getRecords();
        EmployeeLookup lookup = loadEmployeeLookup();
        ExcelAvatarExportUtil.export(
                response,
                "薪资发放.xlsx",
                "薪资发放",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("月份", 12, SalaryRecord::getYearMonth),
                        ExcelAvatarExportUtil.text("工号", 12, row -> ExcelAvatarExportUtil.firstNonBlank(row.getEmpNo(), resolveEmployee(row, lookup).getEmpNo(), "-")),
                        ExcelAvatarExportUtil.text("姓名", 12, row -> resolveEmployeeName(row, lookup)),
                        ExcelAvatarExportUtil.avatar("头像", 12, row -> resolveEmployee(row, lookup).getAvatar()),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getDeptName(), resolveEmployee(row, lookup).getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("基本工资", 14, row -> ExcelAvatarExportUtil.formatNumber(resolveDisplayBaseSalary(row, lookup))),
                        ExcelAvatarExportUtil.text("加班工资", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getOvertimePay())),
                        ExcelAvatarExportUtil.text("绩效奖金", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getPerfBonus())),
                        ExcelAvatarExportUtil.text("津贴", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getAllowance())),
                        ExcelAvatarExportUtil.text("五险一金", 14, row -> formatNegativeNumber(row.getSocialSecurityEmp())),
                        ExcelAvatarExportUtil.text("个税", 12, row -> formatNegativeNumber(row.getIncomeTax())),
                        ExcelAvatarExportUtil.text("扣款金额", 14, row -> formatNegativeNumber(resolveDeductAmount(row))),
                        ExcelAvatarExportUtil.text("实发工资", 14, row -> ExcelAvatarExportUtil.formatNumber(row.getNetSalary())),
                        ExcelAvatarExportUtil.text("审核状态", 12, row -> paymentAuditText(row.getCalcStatus())),
                        ExcelAvatarExportUtil.text("发放日期", 14, row -> ExcelAvatarExportUtil.formatDate(row.getPayDate())),
                        ExcelAvatarExportUtil.text("是否支付", 12, row -> Integer.valueOf(4).equals(row.getCalcStatus()) ? "已支付" : "未支付"),
                        ExcelAvatarExportUtil.text("发布状态", 12, row -> Integer.valueOf(1).equals(row.getSlipPublished()) ? "已发布" : "未发布"),
                        ExcelAvatarExportUtil.text("发放文件", 18, row -> ExcelAvatarExportUtil.firstNonBlank(ExcelAvatarExportUtil.fileNameOf(row.getIssueFile()), "-"))
                ),
                records
        );
    }

    @ApiOperation("获取记录总数")
    @GetMapping("/stat/total")
    public Result<Long> count() {
        return Result.success(salaryService.count());
    }

    @ApiOperation("获取月度总薪资走势（近12个月）")
    @GetMapping("/stat/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(required = false) String yearMonth) {
        return Result.success(salaryService.getMonthlyTrend(yearMonth));
    }

    @ApiOperation("获取指定月份的薪资结构")
    @GetMapping("/stat/structure")
    public Result<Map<String, Object>> getStructure(@RequestParam String yearMonth,
                                                    HttpServletRequest request) {
        Integer role = getRole(request);
        String managerNo = role == 2 ? getUsername(request) : null;
        String excludeEmpNo = null;
        return Result.success(salaryService.getSalaryStructure(yearMonth, managerNo, excludeEmpNo));
    }

    @ApiOperation("获取各部门平均薪资对比")
    @GetMapping("/stat/dept-avg")
    public Result<List<Map<String, Object>>> getDeptAvg(
            @RequestParam(required = false) String yearMonth) {
        return Result.success(salaryService.getDeptAvgSalary(yearMonth));
    }

    @ApiOperation("Get salary detail by id")
    @GetMapping("/{id:[0-9]+}")
    public Result<SalaryRecord> detail(@PathVariable Long id) {
        return Result.success(salaryService.getDetail(id));
    }

    @ApiOperation("Employee: my salary history - only published records (status >= 2)")
    @GetMapping("/my/history")
    public Result<PageResult<SalaryRecord>> myHistory(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(getUserId(request));
        if (employee == null) {
            return Result.success(PageResult.of(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size)));
        }
        return Result.success(salaryService.getMyHistory(current, size, employee.getId()));
    }

    @ApiOperation("Employee: get salary slip for a specific month YYYY-MM")
    @GetMapping("/my/month")
    public Result<SalaryRecord> myMonthSalary(
            @ApiParam("YYYY-MM") @RequestParam String yearMonth,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(getUserId(request));
        if (employee == null) {
            return Result.success(null);
        }
        return Result.success(salaryService.getByEmpAndMonth(employee.getId(), yearMonth));
    }

    @ApiOperation("Calculate salary for one employee")
    @PostMapping("/calculate")
    public Result<SalaryRecord> calculate(
            @RequestParam Long empId,
            @ApiParam("YYYY-MM") @RequestParam String yearMonth) {
        return Result.success(salaryService.calculateSalary(empId, yearMonth));
    }

    @ApiOperation("Batch calculate salary for a month (optional dept filter)")
    @PostMapping("/batch-calculate")
    public Result<Void> batchCalculate(
            @ApiParam("YYYY-MM") @RequestParam String yearMonth,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        Integer role = getRole(request);
        if (role == 2) {
            Long managerId = getUserId(request);
            List<Employee> team = employeeMapper.selectByManagerId(managerId);
            for (Employee emp : team) {
                salaryService.calculateSalary(emp.getId(), yearMonth);
            }
            return Result.successMsg("Manager batch calculate done");
        }
        salaryService.batchCalculate(yearMonth, deptId);
        return Result.successMsg("Batch calculate done");
    }

    @ApiOperation("Publish salary: Draft(1) -> Published(2), employees can now view it")
    @PutMapping("/{id:[0-9]+}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        salaryService.publishSalary(id);
        return Result.successMsg("Published");
    }

    @ApiOperation("Batch publish salary records")
    @PutMapping("/batch-publish")
    public Result<Void> batchPublish(@RequestBody List<Long> ids) {
        salaryService.publishBatch(ids);
        return Result.successMsg("Batch published");
    }

    @ApiOperation("Audit salary: Published(2) -> Audited(3). ADMIN ONLY.")
    @PutMapping("/{id:[0-9]+}/audit")
    public Result<Void> audit(@PathVariable Long id, HttpServletRequest request) {
        salaryService.auditSalary(id, getRole(request));
        return Result.successMsg("Audited");
    }

    @ApiOperation("Batch audit salaries. ADMIN ONLY.")
    @PutMapping("/batch-audit")
    public Result<Void> batchAudit(@RequestBody List<Long> ids, HttpServletRequest request) {
        salaryService.auditBatch(ids, getRole(request));
        return Result.successMsg("Batch audited");
    }

    @ApiOperation("Pay salary: Audited(3) -> Paid(4). ADMIN ONLY (role=1).")
    @PutMapping("/{id:[0-9]+}/pay")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        salaryService.paySalary(id, getUserId(request), getRole(request));
        return Result.successMsg("Payment done");
    }

    @ApiOperation("Batch pay salaries. ADMIN ONLY.")
    @PutMapping("/batch-pay")
    public Result<Void> batchPay(@RequestBody List<Long> ids, HttpServletRequest request) {
        salaryService.payBatch(ids, getUserId(request), getRole(request));
        return Result.successMsg("Batch payment done");
    }

    @ApiOperation("Publish salary slips after payment. ADMIN ONLY.")
    @PutMapping("/{id:[0-9]+}/publish-slip")
    public Result<Void> publishSlip(@PathVariable Long id, HttpServletRequest request) {
        if (getRole(request) != 1) {
            return Result.error("只有管理员才能发布工资条");
        }
        salaryService.publishSalarySlip(id, getUserId(request), getUsername(request));
        return Result.successMsg("Salary slip published");
    }

    @ApiOperation("Batch publish salary slips after payment. ADMIN ONLY.")
    @PutMapping("/batch-publish-slip")
    public Result<Void> batchPublishSlip(@RequestBody List<Long> ids, HttpServletRequest request) {
        if (getRole(request) != 1) {
            return Result.error("只有管理员才能批量发布工资条");
        }
        salaryService.publishSalarySlips(ids, getUserId(request), getUsername(request));
        return Result.successMsg("Batch salary slips published");
    }

    @ApiOperation("Manual update salary record (auto-recalculates gross and net pay)")
    @PutMapping("/manual-update")
    public Result<Void> manualUpdate(@RequestBody SalaryRecord record) {
        salaryService.manualUpdate(record);
        return Result.successMsg("Updated");
    }

    @ApiOperation("删除单条薪资核算记录（仅草稿/已驳回）")
    @DeleteMapping("/{id:[0-9]+}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        salaryService.deleteSalary(id, getRole(request), getUserId(request));
        return Result.successMsg("Deleted");
    }

    @ApiOperation("批量删除薪资核算记录（仅草稿/已驳回）")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids, HttpServletRequest request) {
        salaryService.batchDeleteSalaries(ids, getRole(request), getUserId(request));
        return Result.successMsg("Batch deleted");
    }

    @ApiOperation("上传薪资发放文件（管理员）")
    @PostMapping("/{id:[0-9]+}/issue-file")
    public Result<String> uploadIssueFile(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        if (getRole(request) != 1) {
            return Result.forbidden("只有管理员才能上传发放文件");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请选择发放文件后再上传");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_ISSUE_FILE_EXTENSIONS.contains(normalizedExtension)) {
            return Result.error("仅支持 pdf、doc、docx、xls、xlsx、png、jpg、jpeg、zip 格式");
        }
        try {
            Files.createDirectories(ISSUE_FILE_UPLOAD_DIR);
            String fileName = id + "_" + UUID.randomUUID().toString().replace("-", "") + "." + normalizedExtension;
            Path targetPath = ISSUE_FILE_UPLOAD_DIR.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String issueFileUrl = "/api/uploads/salary-issue/" + fileName;
            salaryService.updateIssueFile(id, issueFileUrl, getRole(request));
            return Result.success("上传成功", issueFileUrl);
        } catch (Exception e) {
            log.error("上传薪资发放文件失败, salaryId={}", id, e);
            return Result.error("上传发放文件失败：" + e.getMessage());
        }
    }

    private RoleScope resolveRoleScope(HttpServletRequest request) {
        Integer role = getRole(request);
        Long managerId = role == 2 ? getUserId(request) : null;
        String excludeEmpNo = null;
        Boolean excludeDraft = role == 1;
        return new RoleScope(managerId, excludeEmpNo, excludeDraft);
    }

    private EmployeeLookup loadEmployeeLookup() {
        Map<Long, Employee> employeeById = new HashMap<>();
        Map<String, Employee> employeeByNo = new HashMap<>();
        for (Employee employee : employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1))) {
            employeeById.put(employee.getId(), employee);
            employeeByNo.put(employee.getEmpNo(), employee);
        }
        return new EmployeeLookup(employeeById, employeeByNo);
    }

    private Employee resolveEmployee(SalaryRecord row, EmployeeLookup lookup) {
        if (row.getEmpId() != null && lookup.employeeById.containsKey(row.getEmpId())) {
            return lookup.employeeById.get(row.getEmpId());
        }
        if (StringUtils.hasText(row.getEmpNo()) && lookup.employeeByNo.containsKey(row.getEmpNo())) {
            return lookup.employeeByNo.get(row.getEmpNo());
        }
        return new Employee();
    }

    private String resolveEmployeeName(SalaryRecord row, EmployeeLookup lookup) {
        return ExcelAvatarExportUtil.firstNonBlank(row.getEmpName(), resolveEmployee(row, lookup).getRealName(), "-");
    }

    private BigDecimal resolveDisplayBaseSalary(SalaryRecord row, EmployeeLookup lookup) {
        BigDecimal salary = row.getBaseSalary();
        if (salary != null && salary.compareTo(BigDecimal.ZERO) > 0) {
            return salary;
        }
        return firstNonNull(resolveEmployee(row, lookup).getBaseSalary(), BigDecimal.ZERO);
    }

    private BigDecimal resolveCalcBase(SalaryRecord row, EmployeeLookup lookup) {
        BigDecimal baseSalary = resolveDisplayBaseSalary(row, lookup);
        if (baseSalary.compareTo(BigDecimal.ZERO) > 0) {
            return baseSalary;
        }
        BigDecimal socialSecurityEmp = firstNonNull(row.getSocialSecurityEmp(), BigDecimal.ZERO);
        if (socialSecurityEmp.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return socialSecurityEmp.divide(new BigDecimal("0.223"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcByRatio(BigDecimal base, String ratio) {
        if (base == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(new BigDecimal(ratio)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveDeductAmount(SalaryRecord row) {
        if (row.getTotalDeduct() != null && row.getTotalDeduct().compareTo(BigDecimal.ZERO) > 0) {
            return row.getTotalDeduct();
        }
        BigDecimal total = BigDecimal.ZERO;
        if (row.getSocialSecurityEmp() != null) {
            total = total.add(row.getSocialSecurityEmp());
        }
        if (row.getAttendDeduct() != null) {
            total = total.add(row.getAttendDeduct());
        }
        if (row.getOtherDeduct() != null) {
            total = total.add(row.getOtherDeduct());
        }
        if (row.getIncomeTax() != null) {
            total = total.add(row.getIncomeTax());
        }
        return total;
    }

    private String formatNegativeNumber(BigDecimal value) {
        BigDecimal amount = firstNonNull(value, BigDecimal.ZERO);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            return "-" + ExcelAvatarExportUtil.formatNumber(amount);
        }
        return ExcelAvatarExportUtil.formatNumber(amount);
    }

    private String salaryStatusText(Integer status) {
        if (status == null) {
            return "-";
        }
        if (status == 1) {
            return "草稿";
        }
        if (status == 2) {
            return "待审核";
        }
        if (status == 3) {
            return "已审核";
        }
        if (status == 4) {
            return "已发放";
        }
        if (status == 5) {
            return "已驳回";
        }
        return "未知";
    }

    private String paymentAuditText(Integer status) {
        if (status == null) {
            return "-";
        }
        return status >= 3 ? "已审核" : "待审核";
    }

    private boolean matchKeyword(String source, String keyword) {
        return !StringUtils.hasText(keyword) || (source != null && source.contains(keyword));
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long getUserId(HttpServletRequest req) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
        return Long.valueOf(c.get("userId").toString());
    }

    private Integer getRole(HttpServletRequest req) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
        return Integer.valueOf(c.get("role").toString());
    }

    private String getUsername(HttpServletRequest req) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
        return String.valueOf(c.get("username"));
    }

    private static final class RoleScope {
        private final Long managerId;
        private final String excludeEmpNo;
        private final Boolean excludeDraft;

        private RoleScope(Long managerId, String excludeEmpNo, Boolean excludeDraft) {
            this.managerId = managerId;
            this.excludeEmpNo = excludeEmpNo;
            this.excludeDraft = excludeDraft;
        }
    }

    private static final class EmployeeLookup {
        private final Map<Long, Employee> employeeById;
        private final Map<String, Employee> employeeByNo;

        private EmployeeLookup(Map<Long, Employee> employeeById, Map<String, Employee> employeeByNo) {
            this.employeeById = employeeById;
            this.employeeByNo = employeeByNo;
        }
    }
}
