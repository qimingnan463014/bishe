package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.entity.SalaryRecord;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.SalaryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Salary Controller
 *
 * Status flow:  Draft(1) -> Published(2) -> Audited(3) -> Paid(4)
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
        String excludeEmpNo = role == 2 ? getUsername(request) : null;
        Boolean excludeDraft = role == 1;
        return Result.success(salaryService.page(
                current, size, yearMonth, empNo, realName, deptId, calcStatus, managerId, excludeEmpNo, excludeDraft));
    }

    @ApiOperation("获取记录总数")
    @GetMapping("/stat/total")
    public Result<Long> count() {
        return Result.success(salaryService.count());
    }

    @ApiOperation("获取月度总薪资走势（近12个月）")
    @GetMapping("/stat/trend")
    public Result<java.util.List<java.util.Map<String, Object>>> getTrend(
            @RequestParam(required = false) String yearMonth) {
        return Result.success(salaryService.getMonthlyTrend(yearMonth));
    }

    @ApiOperation("获取指定月份的薪资构成（雷达图/饼图用）")
    @GetMapping("/stat/structure")
    public Result<java.util.Map<String, Object>> getStructure(@RequestParam String yearMonth) {
        return Result.success(salaryService.getSalaryStructure(yearMonth));
    }

    @ApiOperation("获取各部门平均薪资对比")
    @GetMapping("/stat/dept-avg")
    public Result<java.util.List<java.util.Map<String, Object>>> getDeptAvg(
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
            java.util.List<Employee> team = employeeMapper.selectByManagerId(managerId);
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

    @ApiOperation("Reject salary: Pending Audit(2) -> Rejected(5). ADMIN ONLY.")
    @PutMapping("/{id:[0-9]+}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               HttpServletRequest request) {
        salaryService.rejectSalary(id, getRole(request), reason);
        return Result.successMsg("Rejected");
    }

    @ApiOperation("Batch reject salaries. ADMIN ONLY.")
    @PutMapping("/batch-reject")
    public Result<Void> batchReject(@RequestBody List<Long> ids,
                                    @RequestParam(required = false) String reason,
                                    HttpServletRequest request) {
        salaryService.rejectBatch(ids, getRole(request), reason);
        return Result.successMsg("Batch rejected");
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
}
