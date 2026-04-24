package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.dto.AttendanceSummaryImportDTO;
import com.salary.dto.AttendanceSummaryDTO;
import com.salary.entity.AttendanceRecord;
import com.salary.entity.Employee;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.AttendanceService;
import com.salary.service.SysLogService;
import com.salary.util.ExcelAvatarExportUtil;
import com.salary.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Attendance Controller
 *
 * Excel import clock-in rules (enforced in AttendanceServiceImpl):
 *   morning  > 09:00  -> lateTimes + 1
 *   afternoon < 18:00 -> earlyLeaveTimes + 1
 *   afternoon >= 19:00 -> overtimeHours += floor((out - 19:00) in minutes / 60)
 *   morning  missing  -> skip day entirely, record error message
 *   afternoon missing -> earlyLeave + 1, work hours counted as 4h
 * DB write: upsert by (empId, yearMonth) - UPDATE if exists, INSERT otherwise
 */
@Api(tags = "Attendance Management")
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;
    private final EmployeeMapper employeeMapper;

    @ApiOperation("Page query attendance records (admin=all, manager=own dept)")
    @GetMapping("/page")
    public Result<PageResult<AttendanceRecord>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String recordNo,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) Long deptId,
            HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long managerId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null;
        return Result.success(attendanceService.page(
                current, size, recordNo, yearMonth, empNo, deptId, managerId));
    }

    @ApiOperation("导出考勤 Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String recordNo,
                       @RequestParam(required = false) String yearMonth,
                       @RequestParam(required = false) String empNo,
                       @RequestParam(required = false) String empName,
                       @RequestParam(required = false) Long deptId,
                       HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        Long managerId = role == 2 ? Long.valueOf(c.get("userId").toString()) : null;
        List<AttendanceRecord> records = attendanceService.page(
                1, 50000, recordNo, yearMonth, empNo, deptId, managerId).getRecords();
        Map<Long, Employee> employeeById = new HashMap<>();
        Map<String, Employee> employeeByNo = new HashMap<>();
        for (Employee employee : employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getStatus, 1))) {
            employeeById.put(employee.getId(), employee);
            employeeByNo.put(employee.getEmpNo(), employee);
        }
        records.removeIf(row -> !matchKeyword(resolveEmpName(row, employeeById, employeeByNo), empName));
        ExcelAvatarExportUtil.export(
                response,
                "考勤数据.xlsx",
                "考勤数据",
                Arrays.asList(
                        ExcelAvatarExportUtil.text("登记编号", 16, AttendanceRecord::getRecordNo),
                        ExcelAvatarExportUtil.text("月份", 12, AttendanceRecord::getYearMonth),
                        ExcelAvatarExportUtil.text("工号", 12, row -> ExcelAvatarExportUtil.firstNonBlank(row.getEmpNo(), resolveEmployee(row, employeeById, employeeByNo).getEmpNo(), "-")),
                        ExcelAvatarExportUtil.text("姓名", 12, row -> resolveEmpName(row, employeeById, employeeByNo)),
                        ExcelAvatarExportUtil.avatar("头像", 12, row -> resolveEmployee(row, employeeById, employeeByNo).getAvatar()),
                        ExcelAvatarExportUtil.text("部门", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getDeptName(), resolveEmployee(row, employeeById, employeeByNo).getDeptName(), "-")),
                        ExcelAvatarExportUtil.text("出勤天数", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getAttendDays())),
                        ExcelAvatarExportUtil.text("旷工天数", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getAbsentDays())),
                        ExcelAvatarExportUtil.text("迟到天数", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getLateTimes())),
                        ExcelAvatarExportUtil.text("早退次数", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getEarlyLeaveTimes())),
                        ExcelAvatarExportUtil.text("请假天数", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getLeaveDays())),
                        ExcelAvatarExportUtil.text("加班小时", 12, row -> ExcelAvatarExportUtil.formatNumber(row.getOvertimeHours())),
                        ExcelAvatarExportUtil.text("登记日期", 14, row -> ExcelAvatarExportUtil.formatDate(row.getRecordDate() != null ? row.getRecordDate() : row.getCreateTime())),
                        ExcelAvatarExportUtil.text("经理账号", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerNo(), resolveEmployee(row, employeeById, employeeByNo).getManagerNo(), "-")),
                        ExcelAvatarExportUtil.text("部门经理", 14, row -> ExcelAvatarExportUtil.firstNonBlank(row.getManagerName(), resolveEmployee(row, employeeById, employeeByNo).getManagerName(), "-"))
                ),
                records
        );
    }

    @ApiOperation("获取记录总数")
    @GetMapping("/stat/total")
    public Result<Long> count() {
        return Result.success(attendanceService.count());
    }

    @ApiOperation("获取指定月份的考勤状态分布统计")
    @GetMapping("/stat/status")
    public Result<java.util.Map<String, Object>> getStatus(@RequestParam String yearMonth,
                                                           HttpServletRequest request) {
        Claims c = claims(request);
        Integer role = Integer.valueOf(c.get("role").toString());
        String managerNo = role == 2 ? String.valueOf(c.get("username")) : null;
        String excludeEmpNo = role == 2 ? String.valueOf(c.get("username")) : null;
        return Result.success(attendanceService.getAttendanceStatus(yearMonth, managerNo, excludeEmpNo));
    }

    @ApiOperation("Get attendance record detail")
    @GetMapping("/{id}")
    public Result<AttendanceRecord> detail(@PathVariable Long id) {
        return Result.success(attendanceService.getDetail(id));
    }

    @ApiOperation("Employee: my attendance records (paginated, optional month filter)")
    @GetMapping("/my")
    public Result<PageResult<AttendanceRecord>> myAttendance(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String yearMonth,
            HttpServletRequest request) {
        Employee employee = employeeMapper.selectByUserId(Long.valueOf(claims(request).get("userId").toString()));
        if (employee == null) {
            return Result.success(PageResult.of(new Page<>(current, size)));
        }
        Long empId = employee.getId();
        return Result.success(attendanceService.getMyAttendance(current, size, empId, yearMonth));
    }

    @ApiOperation("Manually add attendance record")
    @PostMapping
    public Result<Void> add(
            @RequestBody AttendanceRecord record,
            HttpServletRequest request) {
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        attendanceService.addRecord(record, managerId);
        return Result.successMsg("Added");
    }

    @ApiOperation("Update attendance record")
    @PutMapping
    public Result<Void> update(@RequestBody AttendanceRecord record) {
        attendanceService.updateRecord(record);
        return Result.successMsg("Updated");
    }

    @ApiOperation("Delete attendance record (blocked if linked to salary)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AttendanceRecord record = attendanceService.getById(id);
        attendanceService.deleteRecord(id);
        Claims claims = claims(request);
        String recordLabel = record == null
                ? "ID=" + id
                : String.format("%s/%s/%s",
                record.getRecordNo() == null ? "-" : record.getRecordNo(),
                record.getEmpName() == null ? "-" : record.getEmpName(),
                record.getYearMonth() == null ? "-" : record.getYearMonth());
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "考勤数据",
                "删除考勤记录[" + recordLabel + "]");
        return Result.successMsg("Deleted");
    }

    /**
     * Import clock-in Excel sheet.
     * Expected columns: empNo | empName | date(yyyy-MM-dd) | morningIn(HH:mm) | afternoonOut(HH:mm)
     * Returns per-employee summary with error messages for malformed rows.
     */
    @ApiOperation("Import clock-in Excel -> auto-analyze -> upsert attendance record")
    @PostMapping(value = "/import/clock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<AttendanceSummaryDTO>> importClock(
            @ApiParam("Excel file .xlsx") @RequestParam("file") MultipartFile file,
            @ApiParam("YYYY-MM") @RequestParam String yearMonth,
            HttpServletRequest request) {
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        return Result.success(attendanceService.importClockExcel(file, yearMonth, managerId));
    }

    @ApiOperation("Import attendance summary Excel -> upsert attendance record")
    @PostMapping(value = "/import/summary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> importSummary(
            @ApiParam("Excel file .xlsx") @RequestParam("file") MultipartFile file,
            @ApiParam("YYYY-MM") @RequestParam String yearMonth,
            HttpServletRequest request) {
        Long managerId = Long.valueOf(claims(request).get("userId").toString());
        attendanceService.importSummaryExcel(file, yearMonth, managerId);
        return Result.successMsg("导入成功");
    }

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }

    private Employee resolveEmployee(AttendanceRecord row,
                                     Map<Long, Employee> employeeById,
                                     Map<String, Employee> employeeByNo) {
        if (row.getEmpId() != null && employeeById.containsKey(row.getEmpId())) {
            return employeeById.get(row.getEmpId());
        }
        if (row.getEmpNo() != null && employeeByNo.containsKey(row.getEmpNo())) {
            return employeeByNo.get(row.getEmpNo());
        }
        return new Employee();
    }

    private String resolveEmpName(AttendanceRecord row,
                                  Map<Long, Employee> employeeById,
                                  Map<String, Employee> employeeByNo) {
        Employee employee = resolveEmployee(row, employeeById, employeeByNo);
        return ExcelAvatarExportUtil.firstNonBlank(row.getEmpName(), employee.getRealName(), "-");
    }

    private boolean matchKeyword(String source, String keyword) {
        return !org.springframework.util.StringUtils.hasText(keyword)
                || (source != null && source.contains(keyword));
    }
}
