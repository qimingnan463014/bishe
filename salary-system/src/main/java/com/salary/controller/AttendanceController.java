package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.dto.AttendanceSummaryDTO;
import com.salary.entity.AttendanceRecord;
import com.salary.entity.Employee;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.AttendanceService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
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
import java.util.List;
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

    @ApiOperation("获取记录总数")
    @GetMapping("/stat/total")
    public Result<Long> count() {
        return Result.success(attendanceService.count());
    }

    @ApiOperation("获取指定月份的考勤状态分布统计")
    @GetMapping("/stat/status")
    public Result<java.util.Map<String, Object>> getStatus(@RequestParam String yearMonth) {
        return Result.success(attendanceService.getAttendanceStatus(yearMonth));
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

    private Claims claims(HttpServletRequest req) {
        return jwtUtil.parseToken(jwtUtil.extractToken(req.getHeader("Authorization")));
    }
}
