package com.salary.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.dto.AttendanceClockDTO;
import com.salary.dto.AttendanceSummaryImportDTO;
import com.salary.dto.AttendanceSummaryDTO;
import com.salary.entity.AttendanceRecord;
import com.salary.entity.AttendanceRule;
import com.salary.entity.Department;
import com.salary.entity.Employee;
import com.salary.entity.User;
import com.salary.mapper.AttendanceRecordMapper;
import com.salary.mapper.AttendanceRuleMapper;
import com.salary.mapper.DepartmentMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.mapper.SalaryRecordMapper;
import com.salary.mapper.UserMapper;
import com.salary.service.AttendanceService;
import com.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤 ServiceImpl
 * <p>
 * 核心功能：
 * 1. 分页查询 / CRUD
 * 2. 员工提交申诉
 * 3. ★ 从 Excel 打卡明细自动判定并汇总考勤
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord>
        implements AttendanceService {

    // ====================================================
    // 考勤判定常量（可后续移入配置表 t_attendance_rule）
    // ====================================================
    /** 规定上班时间 09:00（晚到则迟到） */
    private static final LocalTime WORK_START      = LocalTime.of(9, 0);
    /** 规定下班时间 18:00（早走则早退） */
    private static final LocalTime WORK_END        = LocalTime.of(18, 0);
    /** 早退判定阈值 17:50（预留10分钟弹性） */
    private static final LocalTime EARLY_LEAVE_CUTOFF = LocalTime.of(17, 50);
    /** 加班起算时间 19:00（>= 此时间才计加班） */
    private static final LocalTime OVERTIME_START  = LocalTime.of(19, 0);
    /** 打卡时间格式 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    /** 日期格式 */
    // DATE_FMT removed (unused)
    private static final BigDecimal DEFAULT_LEAVE_DEDUCT_RATIO = new BigDecimal("1.0");
    private static final BigDecimal DEFAULT_SICK_LEAVE_DEDUCT_RATIO = new BigDecimal("0.5");
    private static final int DEFAULT_WORK_DAYS = 22;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int LEAVE_TYPE_PERSONAL = 1;
    private static final int LEAVE_TYPE_SICK = 2;
    private static final int SIGN_TYPE_MORNING = 1;
    private static final int SIGN_TYPE_AFTERNOON = 2;
    private static final int SIGN_TYPE_FULL_DAY = 3;

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final SalaryRecordMapper     salaryRecordMapper;
    private final EmployeeMapper         employeeMapper;
    private final AttendanceRuleMapper   attendanceRuleMapper;
    private final DepartmentMapper       departmentMapper;
    private final UserMapper             userMapper;
    private final SalaryService          salaryService;

    // ====================================================
    //  查询 / 基础 CRUD
    // ====================================================

    @Override
    public PageResult<AttendanceRecord> page(int current, int size,
                                              String recordNo, String yearMonth,
                                              String empNo, Long deptId, Long managerId) {
        Page<AttendanceRecord> page = new Page<>(current, size);
        return PageResult.of(attendanceRecordMapper.selectPageWithCondition(
                page, recordNo, yearMonth, empNo, deptId, managerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRecord(AttendanceRecord record, Long managerId) {
        long exist = lambdaQuery()
                .eq(AttendanceRecord::getEmpId, record.getEmpId())
                .eq(AttendanceRecord::getYearMonth, record.getYearMonth())
                .count();
        if (exist > 0) {
            throw new RuntimeException("员工 " + record.getEmpNo()
                    + " 已存在 " + record.getYearMonth() + " 考勤记录，请勿重复录入");
        }
        record.setRecordNo(generateRecordNo(record.getYearMonth(), record.getEmpNo()));
        record.setManagerId(managerId);
        fillAttendanceDeduct(record);
        save(record);
        recalculateSalaryAfterManualAttendanceChange(record.getEmpId(), record.getYearMonth());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(AttendanceRecord record) {
        AttendanceRecord exist = getById(record.getId());
        if (exist == null) throw new RuntimeException("考勤记录不存在");
        if (exist.getStatus() != null && exist.getStatus() == 3)
            throw new RuntimeException("该考勤已锁定，禁止修改");
        fillAttendanceDeduct(record);
        updateById(record);
        Long targetEmpId = record.getEmpId() != null ? record.getEmpId() : exist.getEmpId();
        String targetYearMonth = StringUtils.hasText(record.getYearMonth()) ? record.getYearMonth() : exist.getYearMonth();
        recalculateSalaryAfterManualAttendanceChange(targetEmpId, targetYearMonth);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        AttendanceRecord record = getById(id);
        if (record == null) throw new RuntimeException("考勤记录不存在");
        long salaryRef = salaryRecordMapper.selectCount(
                new LambdaQueryWrapper<com.salary.entity.SalaryRecord>()
                        .eq(com.salary.entity.SalaryRecord::getAttendanceId, id));
        if (salaryRef > 0) throw new RuntimeException("该考勤已关联薪资，禁止删除");
        removeById(id);
    }

    @Override
    public AttendanceRecord getDetail(Long id) {
        return getById(id);
    }

    @Override
    public PageResult<AttendanceRecord> getMyAttendance(int current, int size,
                                                         Long empId, String yearMonth) {
        Page<AttendanceRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<AttendanceRecord> qw = new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getEmpId, empId)
                .eq(StringUtils.hasText(yearMonth), AttendanceRecord::getYearMonth, yearMonth)
                .orderByDesc(AttendanceRecord::getYearMonth);
        return PageResult.of(attendanceRecordMapper.selectPage(page, qw));
    }

    // ====================================================
    //  ★ 核心：Excel 打卡明细导入 → 自动判定 → 汇总写库
    // ====================================================

    /**
     * 导入考勤打卡 Excel，自动分析判定并写入 t_attendance_record
     * <p>
     * Excel 每行格式：工号 | 姓名 | 日期(yyyy-MM-dd) | 上午打卡时间(HH:mm) | 下午打卡时间(HH:mm)
     *
     * @param file      上传的 Excel 文件
     * @param yearMonth 对应年月（格式：YYYY-MM），用于确定汇总周期
     * @param managerId 录入经理的用户 ID
     * @return 每个员工的汇总结果（含异常行提示）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AttendanceSummaryDTO> importClockExcel(MultipartFile file,
                                                        String yearMonth,
                                                        Long managerId) {
        // 1. 用 EasyExcel 读取所有打卡明细行
        List<AttendanceClockDTO> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), AttendanceClockDTO.class,
                    new ReadListener<AttendanceClockDTO>() {
                        @Override
                        public void invoke(AttendanceClockDTO data, AnalysisContext ctx) {
                            rows.add(data);
                        }
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {
                            log.info("考勤Excel解析完毕，共{}行", rows.size());
                        }
                    }).sheet().doRead();
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败：" + e.getMessage(), e);
        }

        // 2. 按工号分组 → 逐员工判定汇总
        Map<String, List<AttendanceClockDTO>> grouped =
                rows.stream().collect(Collectors.groupingBy(AttendanceClockDTO::getEmpNo));

        List<AttendanceSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<AttendanceClockDTO>> entry : grouped.entrySet()) {
            AttendanceSummaryDTO summary = analyzeClockRows(entry.getValue(), yearMonth);
            summaries.add(summary);

            // 3. 若无异常，写入 / 更新数据库
            if (summary.getErrorMessages() == null || summary.getErrorMessages().isEmpty()) {
                saveOrUpdateAttendanceRecord(summary, yearMonth, managerId);
            } else {
                log.warn("员工 {} 考勤Excel存在异常，跳过入库：{}", summary.getEmpNo(), summary.getErrorMessages());
            }
        }
        return summaries;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importSummaryExcel(MultipartFile file, String yearMonth, Long managerId) {
        List<AttendanceSummaryImportDTO> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), AttendanceSummaryImportDTO.class,
                    new ReadListener<AttendanceSummaryImportDTO>() {
                        @Override
                        public void invoke(AttendanceSummaryImportDTO data, AnalysisContext ctx) {
                            rows.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext ctx) {
                            log.info("考勤汇总Excel解析完毕，共{}行", rows.size());
                        }
                    }).sheet().doRead();
        } catch (Exception e) {
            throw new RuntimeException("考勤汇总Excel解析失败：" + e.getMessage(), e);
        }

        for (AttendanceSummaryImportDTO row : rows) {
            if (!StringUtils.hasText(row.getEmpNo())) {
                continue;
            }
            upsertSummaryAttendanceRecord(row, yearMonth, managerId);
        }
    }

    // ====================================================
    //  打卡时间判定逻辑（核心算法）
    // ====================================================

    /**
     * 分析单个员工的所有打卡明细行，汇总出当月考勤结果
     *
     * <pre>
     * 判定规则（以天为单位逐行处理）：
     *   1. 上午打卡时间解析失败 / 缺失 → 视为旷工半天（absentDays + 0.5）[或记录为错误]
     *   2. 上午打卡时间 > 09:00         → lateTimes + 1
     *   3. 下午打卡时间解析失败 / 缺失 → 视为早退（earlyLeaveTimes + 1）
     *   4. 下午打卡时间 < 18:00         → earlyLeaveTimes + 1
     *   5. 下午打卡时间 >= 19:00        → overtimeHours + floor((打卡时间 - 19:00) 的分钟数 / 60)
     * </pre>
     */
    private AttendanceSummaryDTO analyzeClockRows(List<AttendanceClockDTO> rows, String yearMonth) {
        AttendanceSummaryDTO summary = new AttendanceSummaryDTO();
        summary.setEmpNo(rows.get(0).getEmpNo());
        summary.setEmpName(rows.get(0).getEmpName());
        summary.setYearMonth(yearMonth);
        summary.setAbsentDays(BigDecimal.ZERO);
        summary.setLeaveDays(BigDecimal.ZERO);
        summary.setErrorMessages(new ArrayList<>());

        int lateTimes = 0;
        int earlyLeaveTimes = 0;
        long overtimeMinutes = 0L;
        int attendDays = 0;
        double totalWorkHours = 0.0;

        for (AttendanceClockDTO row : rows) {
            String dateStr = row.getWorkDate();

            // ---- 解析上午打卡时间 ----
            LocalTime morningIn = parseTime(row.getMorningClockIn());
            if (morningIn == null) {
                // 上午打卡缺失 → 本天视为旷工（不计入出勤）
                summary.getErrorMessages().add("日期 " + dateStr + " 上午打卡时间格式异常或缺失（"
                        + row.getMorningClockIn() + "），已跳过该天");
                continue; // 跳过整天
            }

            attendDays++; // 有打卡记录才计入出勤

            // 迟到判定：上午打卡时间 > 09:00
            if (morningIn.isAfter(WORK_START)) {
                lateTimes++;
                log.debug("迟到：empNo={}, date={}, morningIn={}", row.getEmpNo(), dateStr, morningIn);
            }

            // ---- 解析下午打卡时间 ----
            LocalTime afternoonOut = parseTime(row.getAfternoonClockOut());
            if (afternoonOut == null) {
                // 下午打卡缺失 → 记早退
                earlyLeaveTimes++;
                summary.getErrorMessages().add("日期 " + dateStr + " 下午打卡时间格式异常或缺失（"
                        + row.getAfternoonClockOut() + "），已记为早退");
                // 本天出勤时长按半天计（4小时）
                totalWorkHours += 4.0;
                continue;
            }

            // 早退判定：下午打卡时间 < 17:50（保留10分钟弹性）
            if (afternoonOut.isBefore(EARLY_LEAVE_CUTOFF)) {
                earlyLeaveTimes++;
                log.debug("早退：empNo={}, date={}, afternoonOut={}", row.getEmpNo(), dateStr, afternoonOut);
            }
            // 加班判定：下午打卡时间 >= 19:00
            else if (!afternoonOut.isBefore(OVERTIME_START)) {
                // 加班时长 = (打卡时间 - 19:00) 的整分钟数，向下取整为小时
                long overtimeMin = toMinutes(afternoonOut) - toMinutes(OVERTIME_START);
                long overtimeHrs = overtimeMin / 60; // 向下取整，例如 50 分钟 → 0 小时，70 分钟 → 1 小时
                overtimeMinutes += overtimeMin; // 先累计分钟，最后统一转换
                log.debug("加班：empNo={}, date={}, out={}, overtimeMin={}, overtimeHrs={}",
                        row.getEmpNo(), dateStr, afternoonOut, overtimeMin, overtimeHrs);
            }

            // 本天出勤时长（上下班时差，最多计8小时，不含加班）
            long workMin = Math.min(toMinutes(afternoonOut) - toMinutes(morningIn),
                    toMinutes(WORK_END) - toMinutes(WORK_START));
            totalWorkHours += Math.max(0, workMin) / 60.0;
        }

        // 加班总小时数（整小时向下取整）
        long totalOvertimeHours = overtimeMinutes / 60;

        summary.setAttendDays(attendDays);
        summary.setLateTimes(lateTimes);
        summary.setEarlyLeaveTimes(earlyLeaveTimes);
        summary.setOvertimeHours(new BigDecimal(totalOvertimeHours));
        // 出勤总时长 = 正常上班时长 + 加班小时
        summary.setAttendHours(BigDecimal.valueOf(totalWorkHours + totalOvertimeHours)
                .setScale(1, java.math.RoundingMode.HALF_UP));

        log.info("考勤汇总：empNo={} yearMonth={} 出勤{}天 迟到{}次 早退{}次 加班{}小时",
                summary.getEmpNo(), yearMonth, attendDays, lateTimes, earlyLeaveTimes, totalOvertimeHours);
        return summary;
    }

    /**
     * 将汇总结果写入 / 更新 t_attendance_record
     * 若该员工该月已有记录则更新（幂等），否则新增
     */
    private void saveOrUpdateAttendanceRecord(AttendanceSummaryDTO summary,
                                               String yearMonth,
                                               Long managerId) {
        // 通过工号查找员工
        Employee emp = employeeMapper.selectByEmpNo(summary.getEmpNo());
        if (emp == null) {
            log.warn("员工不存在，跳过写库：empNo={}", summary.getEmpNo());
            return;
        }

        // 查询是否已有该月考勤记录
        AttendanceRecord exist = attendanceRecordMapper.selectByEmpAndMonth(emp.getId(), yearMonth);

        AttendanceRecord record = exist != null ? exist : new AttendanceRecord();
        record.setEmpId(emp.getId());
        record.setEmpNo(emp.getEmpNo());
        record.setEmpName(emp.getRealName());
        record.setDeptId(emp.getDeptId());
        record.setDeptName(emp.getDeptName() == null ? "" : emp.getDeptName());
        record.setYearMonth(yearMonth);
        record.setAttendDays(summary.getAttendDays());
        // 表结构当前仅保留 late_times，按需求将“迟到+早退”统一计入该字段参与扣款。
        int mergedLateTimes = summary.getLateTimes() + summary.getEarlyLeaveTimes();
        record.setLateTimes(mergedLateTimes);
        record.setAbsentDays(summary.getAbsentDays());
        record.setLeaveDays(summary.getLeaveDays());
        record.setSickLeaveDays(BigDecimal.ZERO);
        record.setOvertimeHours(summary.getOvertimeHours());
        record.setAttendHours(summary.getAttendHours());
        record.setRecordDate(LocalDate.now());
        record.setManagerId(managerId);
        record.setManagerNo(emp.getManagerNo());
        record.setManagerName(emp.getManagerName());
        record.setStatus(1); // 正常
        int normalClockDays = Math.max(summary.getAttendDays() - summary.getLateTimes() - summary.getEarlyLeaveTimes(), 0);
        fillAttendanceDeduct(record);
        record.setRemark(String.format("系统导入：迟到%d次；早退%d次；正常打卡%d天；加班%s小时；考勤扣款%s元",
                summary.getLateTimes(),
                summary.getEarlyLeaveTimes(),
                normalClockDays,
                summary.getOvertimeHours() == null ? "0" : summary.getOvertimeHours().toPlainString(),
                record.getAttendDeduct() == null ? "0.00" : record.getAttendDeduct().toPlainString()));

        if (exist == null) {
            record.setRecordNo(generateRecordNo(record.getYearMonth(), record.getEmpNo()));
            save(record);
            log.info("新增考勤记录：empNo={} yearMonth={}", summary.getEmpNo(), yearMonth);
        } else {
            updateById(record);
            log.info("更新考勤记录：empNo={} yearMonth={}", summary.getEmpNo(), yearMonth);
        }
    }

    private void upsertSummaryAttendanceRecord(AttendanceSummaryImportDTO summary,
                                               String yearMonth,
                                               Long managerId) {
        Employee emp = employeeMapper.selectByEmpNo(summary.getEmpNo());
        if (emp == null) {
            throw new RuntimeException("员工不存在，无法导入考勤汇总：工号=" + summary.getEmpNo());
        }

        AttendanceRecord exist = attendanceRecordMapper.selectByEmpAndMonth(emp.getId(), yearMonth);
        AttendanceRecord record = exist != null ? exist : new AttendanceRecord();
        record.setEmpId(emp.getId());
        record.setEmpNo(emp.getEmpNo());
        record.setEmpName(StringUtils.hasText(summary.getEmpName()) ? summary.getEmpName().trim() : emp.getRealName());
        record.setDeptId(emp.getDeptId());
        record.setDeptName(defaultText(emp.getDeptName()));
        record.setYearMonth(yearMonth);
        record.setAttendDays(parseInteger(summary.getAttendDays()));
        record.setAbsentDays(parseDecimal(summary.getAbsentDays()));
        record.setLateTimes(parseInteger(summary.getLateTimes()));
        record.setEarlyLeaveTimes(parseInteger(summary.getEarlyLeaveTimes()));
        record.setLeaveDays(parseDecimal(summary.getLeaveDays()));
        record.setSickLeaveDays(ZERO.setScale(2, RoundingMode.HALF_UP));
        record.setOvertimeHours(parseDecimal(summary.getOvertimeHours()));
        record.setAttendHours(calculateAttendHours(record));
        record.setRecordDate(LocalDate.now());
        applyManagerSnapshot(record, emp, managerId);
        record.setStatus(1);
        fillAttendanceDeduct(record);
        record.setRemark(String.format("Excel汇总导入：出勤%d天；旷工%s天；迟到%d次；早退%d次；请假%s天；加班%s小时；考勤扣款%s元",
                record.getAttendDays() == null ? 0 : record.getAttendDays(),
                record.getAbsentDays() == null ? "0.00" : record.getAbsentDays().toPlainString(),
                record.getLateTimes() == null ? 0 : record.getLateTimes(),
                record.getEarlyLeaveTimes() == null ? 0 : record.getEarlyLeaveTimes(),
                record.getLeaveDays() == null ? "0.00" : record.getLeaveDays().toPlainString(),
                record.getOvertimeHours() == null ? "0.00" : record.getOvertimeHours().toPlainString(),
                record.getAttendDeduct() == null ? "0.00" : record.getAttendDeduct().toPlainString()));

        if (exist == null) {
            record.setRecordNo(generateRecordNo(record.getYearMonth(), record.getEmpNo()));
            save(record);
            log.info("新增考勤汇总记录：empNo={} yearMonth={}", record.getEmpNo(), yearMonth);
        } else {
            updateById(record);
            log.info("更新考勤汇总记录：empNo={} yearMonth={}", record.getEmpNo(), yearMonth);
        }
    }

    // ====================================================
    //  工具方法
    // ====================================================

    /** 将 LocalTime 转为分钟数（方便计算差值） */
    private long toMinutes(LocalTime t) {
        return t.getHour() * 60L + t.getMinute();
    }

    /**
     * 安全解析 HH:mm 格式时间，失败返回 null
     */
    private LocalTime parseTime(String timeStr) {
        if (!StringUtils.hasText(timeStr)) return null;
        try {
            // 支持 "HH:mm" 和 "H:mm"（如 "9:05"）
            String normalized = timeStr.trim().length() == 4 ? "0" + timeStr.trim() : timeStr.trim();
            return LocalTime.parse(normalized, TIME_FMT);
        } catch (DateTimeParseException e) {
            log.warn("时间格式解析失败：'{}'", timeStr);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return new BigDecimal(value.trim()).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (Exception e) {
            throw new RuntimeException("数字格式错误：" + value);
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new RuntimeException("数字格式错误：" + value);
        }
    }

    private BigDecimal calculateAttendHours(AttendanceRecord record) {
        int attendDays = record.getAttendDays() == null ? 0 : record.getAttendDays();
        BigDecimal overtimeHours = nvl(record.getOvertimeHours(), ZERO);
        return BigDecimal.valueOf(attendDays)
                .multiply(new BigDecimal("8"))
                .add(overtimeHours)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private void applyManagerSnapshot(AttendanceRecord record, Employee emp, Long operatorUserId) {
        record.setManagerId(emp.getManagerId());
        record.setManagerNo(defaultText(emp.getManagerNo()));
        record.setManagerName(defaultText(emp.getManagerName()));
        if (record.getManagerId() != null || (hasText(record.getManagerNo()) && hasText(record.getManagerName()))) {
            return;
        }
        if (operatorUserId == null) {
            return;
        }
        User operator = userMapper.selectById(operatorUserId);
        if (operator == null || operator.getRole() == null || operator.getRole() != 2) {
            return;
        }
        record.setManagerId(operatorUserId);
        record.setManagerNo(defaultText(operator.getUsername()));
        record.setManagerName(defaultText(operator.getRealName()));
    }

    /** 生成统一的考勤登记编号：ATTYYYYMM-工号 */
    private String generateRecordNo(String yearMonth, String empNo) {
        String normalizedYearMonth = hasText(yearMonth)
                ? yearMonth.replace("-", "").trim()
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String normalizedEmpNo = hasText(empNo) ? empNo.trim() : "UNKNOWN";
        return "ATT" + normalizedYearMonth + "-" + normalizedEmpNo;
    }

    @Override
    public java.util.Map<String, Object> getAttendanceStatus(String yearMonth, String managerNo, String excludeEmpNo) {
        return attendanceRecordMapper.countAttendanceStatus(yearMonth, managerNo, excludeEmpNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord syncApprovedLeaveToAttendance(com.salary.entity.AttendanceApply apply,
                                                          Long reviewerId,
                                                          String reviewerName) {
        if (apply == null || !Integer.valueOf(2).equals(apply.getApplyType())) {
            return null;
        }
        if (apply.getEmpId() == null || apply.getApplyDate() == null) {
            throw new RuntimeException("请假申请缺少员工或日期信息，无法同步考勤");
        }
        BigDecimal actualLeaveDays = nvl(apply.getLeaveDays(), ZERO).setScale(2, RoundingMode.HALF_UP);
        if (actualLeaveDays.compareTo(ZERO) <= 0) {
            throw new RuntimeException("请假申请天数必须大于0");
        }

        AttendanceRecord record = prepareApplyAttendanceRecord(apply, reviewerId, reviewerName);
        BigDecimal deductedLeaveDays = toDeductedLeaveDays(apply.getLeaveType(), actualLeaveDays);
        record.setLeaveDays(nvl(record.getLeaveDays(), ZERO).add(deductedLeaveDays).setScale(2, RoundingMode.HALF_UP));
        if (Integer.valueOf(LEAVE_TYPE_SICK).equals(apply.getLeaveType())) {
            record.setSickLeaveDays(nvl(record.getSickLeaveDays(), ZERO).add(actualLeaveDays).setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setSickLeaveDays(nvl(record.getSickLeaveDays(), ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        record.setRemark(buildLeaveSyncRemark(record, apply, actualLeaveDays, deductedLeaveDays));
        return persistApplyAttendanceRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord syncApprovedSignToAttendance(com.salary.entity.AttendanceApply apply,
                                                         Long reviewerId,
                                                         String reviewerName) {
        if (apply == null || !Integer.valueOf(1).equals(apply.getApplyType())) {
            return null;
        }
        Integer signType = apply.getSignType();
        if (signType == null || signType < 1 || signType > 3) {
            throw new RuntimeException("补签申请缺少有效的补签类型");
        }
        AttendanceRecord record = prepareApplyAttendanceRecord(apply, reviewerId, reviewerName);
        int lateTimes = record.getLateTimes() == null ? 0 : record.getLateTimes();
        int earlyLeaveTimes = record.getEarlyLeaveTimes() == null ? 0 : record.getEarlyLeaveTimes();
        BigDecimal absentDays = nvl(record.getAbsentDays(), ZERO);

        if (signType == SIGN_TYPE_MORNING) {
            record.setLateTimes(Math.max(0, lateTimes - 1));
        } else if (signType == SIGN_TYPE_AFTERNOON) {
            record.setEarlyLeaveTimes(Math.max(0, earlyLeaveTimes - 1));
        } else {
            record.setAttendDays((record.getAttendDays() == null ? 0 : record.getAttendDays()) + 1);
            if (absentDays.compareTo(ZERO) > 0) {
                record.setAbsentDays(absentDays.subtract(ONE).max(ZERO).setScale(2, RoundingMode.HALF_UP));
            }
            record.setLateTimes(Math.max(0, lateTimes - 1));
            record.setEarlyLeaveTimes(Math.max(0, earlyLeaveTimes - 1));
        }

        record.setRemark(buildSignSyncRemark(record, apply));
        return persistApplyAttendanceRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord syncApprovedOvertimeToAttendance(com.salary.entity.AttendanceApply apply,
                                                             Long reviewerId,
                                                             String reviewerName) {
        if (apply == null || !Integer.valueOf(3).equals(apply.getApplyType())) {
            return null;
        }
        BigDecimal overtimeHours = nvl(apply.getOvertimeHours(), ZERO).setScale(2, RoundingMode.HALF_UP);
        if (overtimeHours.compareTo(ZERO) <= 0) {
            throw new RuntimeException("加班申请缺少有效的加班时长");
        }
        AttendanceRecord record = prepareApplyAttendanceRecord(apply, reviewerId, reviewerName);
        record.setOvertimeHours(nvl(record.getOvertimeHours(), ZERO).add(overtimeHours).setScale(2, RoundingMode.HALF_UP));
        record.setRemark(buildOvertimeSyncRemark(record, apply, overtimeHours));
        return persistApplyAttendanceRecord(record);
    }

    private void fillAttendanceDeduct(AttendanceRecord record) {
        if (record == null) {
            return;
        }
        record.setAttendHours(calculateAttendHours(record));
        AttendanceRule rule = resolveActiveAttendanceRule();
        BigDecimal baseSalary = resolveEmployeeBaseSalary(record.getEmpId());
        BigDecimal daySalary = resolveDaySalary(baseSalary, rule);
        BigDecimal absentDeduct = resolveAbsentDeduct(record, daySalary, rule);
        BigDecimal leaveDeduct = resolveLeaveDeduct(record, daySalary, rule);
        int lateTimes = record.getLateTimes() == null ? 0 : record.getLateTimes();
        int earlyLeaveTimes = record.getEarlyLeaveTimes() == null ? 0 : record.getEarlyLeaveTimes();
        BigDecimal lateDeduct = nvl(rule.getLateDeductPerTime(), new BigDecimal("50"))
                .multiply(BigDecimal.valueOf(lateTimes + earlyLeaveTimes));
        BigDecimal totalDeduct = absentDeduct
                .add(leaveDeduct)
                .add(lateDeduct)
                .setScale(2, RoundingMode.HALF_UP);
        record.setAttendDeduct(totalDeduct);
    }

    private void recalculateSalaryAfterManualAttendanceChange(Long empId, String yearMonth) {
        if (empId == null || !StringUtils.hasText(yearMonth)) {
            return;
        }
        salaryService.calculateSalary(empId, yearMonth);
    }

    private AttendanceRule resolveActiveAttendanceRule() {
        AttendanceRule rule = attendanceRuleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getIsActive, 1)
                .orderByDesc(AttendanceRule::getEffectiveDate)
                .last("LIMIT 1"));
        if (rule != null) {
            return rule;
        }
        AttendanceRule fallback = new AttendanceRule();
        fallback.setWorkDays(DEFAULT_WORK_DAYS);
        fallback.setLateDeductPerTime(new BigDecimal("50"));
        fallback.setLeaveDeductRatio(DEFAULT_LEAVE_DEDUCT_RATIO);
        fallback.setSickLeaveDeductRatio(DEFAULT_SICK_LEAVE_DEDUCT_RATIO);
        fallback.setAbsentDeductPerDay(BigDecimal.ZERO);
        return fallback;
    }

    private BigDecimal resolveEmployeeBaseSalary(Long empId) {
        if (empId == null) {
            return ZERO;
        }
        Employee emp = employeeMapper.selectById(empId);
        if (emp == null || emp.getBaseSalary() == null) {
            return ZERO;
        }
        return emp.getBaseSalary();
    }

    private BigDecimal resolveDaySalary(BigDecimal baseSalary, AttendanceRule rule) {
        int workDays = rule.getWorkDays() == null || rule.getWorkDays() <= 0 ? DEFAULT_WORK_DAYS : rule.getWorkDays();
        return nvl(baseSalary, ZERO)
                .divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveAbsentDeduct(AttendanceRecord record, BigDecimal daySalary, AttendanceRule rule) {
        BigDecimal absentDays = nvl(record.getAbsentDays(), ZERO);
        if (absentDays.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        BigDecimal absentDeductPerDay = nvl(rule.getAbsentDeductPerDay(), ZERO);
        BigDecimal unitDeduct = absentDeductPerDay.compareTo(ZERO) > 0 ? absentDeductPerDay : daySalary;
        return unitDeduct.multiply(absentDays);
    }

    private BigDecimal resolveLeaveDeduct(AttendanceRecord record, BigDecimal daySalary, AttendanceRule rule) {
        BigDecimal leaveDays = nvl(record.getLeaveDays(), ZERO);
        if (leaveDays.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return daySalary.multiply(leaveDays);
    }

    private BigDecimal nvl(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal toDeductedLeaveDays(Integer leaveType, BigDecimal actualLeaveDays) {
        if (actualLeaveDays == null) {
            return ZERO;
        }
        if (Integer.valueOf(LEAVE_TYPE_SICK).equals(leaveType)) {
            return actualLeaveDays.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
        }
        return actualLeaveDays.setScale(2, RoundingMode.HALF_UP);
    }

    private AttendanceRecord prepareApplyAttendanceRecord(com.salary.entity.AttendanceApply apply,
                                                          Long reviewerId,
                                                          String reviewerName) {
        if (apply.getEmpId() == null || apply.getApplyDate() == null) {
            throw new RuntimeException("申请缺少员工或日期信息，无法同步考勤");
        }
        Employee emp = employeeMapper.selectById(apply.getEmpId());
        if (emp == null) {
            throw new RuntimeException("员工不存在，无法同步申请到考勤");
        }
        String yearMonth = apply.getApplyDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        AttendanceRecord existing = attendanceRecordMapper.selectByEmpAndMonth(emp.getId(), yearMonth);
        AttendanceRecord record = existing != null ? existing : new AttendanceRecord();
        Department dept = emp.getDeptId() == null ? null : departmentMapper.selectById(emp.getDeptId());
        Long recordManagerId = emp.getManagerId() != null ? emp.getManagerId() : (dept != null ? dept.getManagerId() : null);
        User recordManager = recordManagerId == null ? null : userMapper.selectById(recordManagerId);

        record.setEmpId(emp.getId());
        record.setEmpNo(hasText(record.getEmpNo()) ? record.getEmpNo() : defaultText(emp.getEmpNo()));
        record.setEmpName(hasText(record.getEmpName()) ? record.getEmpName() : defaultText(emp.getRealName()));
        record.setDeptId(emp.getDeptId());
        if (!hasText(record.getDeptName())) {
            record.setDeptName(dept != null ? defaultText(dept.getDeptName()) : "");
        }
        record.setYearMonth(yearMonth);
        record.setAttendDays(record.getAttendDays() == null ? 0 : record.getAttendDays());
        record.setAbsentDays(nvl(record.getAbsentDays(), ZERO));
        record.setLateTimes(record.getLateTimes() == null ? 0 : record.getLateTimes());
        record.setEarlyLeaveTimes(record.getEarlyLeaveTimes() == null ? 0 : record.getEarlyLeaveTimes());
        record.setLeaveDays(nvl(record.getLeaveDays(), ZERO));
        record.setSickLeaveDays(nvl(record.getSickLeaveDays(), ZERO));
        record.setOvertimeHours(nvl(record.getOvertimeHours(), ZERO));
        record.setAttendHours(nvl(record.getAttendHours(), ZERO));
        record.setRecordDate(record.getRecordDate() == null ? apply.getApplyDate() : record.getRecordDate());
        record.setManagerId(recordManagerId);
        if (recordManager != null) {
            record.setManagerNo(defaultText(recordManager.getUsername()));
            record.setManagerName(defaultText(recordManager.getRealName()));
        } else if (recordManagerId == null) {
            record.setManagerNo(hasText(record.getManagerNo()) ? record.getManagerNo() : "");
            record.setManagerName(hasText(record.getManagerName()) ? record.getManagerName() : "");
        }
        record.setStatus(record.getStatus() == null ? 1 : record.getStatus());
        return record;
    }

    private AttendanceRecord persistApplyAttendanceRecord(AttendanceRecord record) {
        fillAttendanceDeduct(record);
        if (record.getId() == null) {
            record.setRecordNo(generateRecordNo(record.getYearMonth(), record.getEmpNo()));
            save(record);
        } else {
            updateById(record);
        }
        return record;
    }

    private String buildSignSyncRemark(AttendanceRecord record, com.salary.entity.AttendanceApply apply) {
        String signLabel;
        Integer signType = apply.getSignType();
        if (Integer.valueOf(SIGN_TYPE_MORNING).equals(signType)) {
            signLabel = "上午补签";
        } else if (Integer.valueOf(SIGN_TYPE_AFTERNOON).equals(signType)) {
            signLabel = "下午补签";
        } else {
            signLabel = "全天补签";
        }
        String base = String.format("审批同步：%s；当前出勤%d天，迟到%d次，早退%d次，旷工%s天。",
                signLabel,
                record.getAttendDays() == null ? 0 : record.getAttendDays(),
                record.getLateTimes() == null ? 0 : record.getLateTimes(),
                record.getEarlyLeaveTimes() == null ? 0 : record.getEarlyLeaveTimes(),
                nvl(record.getAbsentDays(), ZERO).stripTrailingZeros().toPlainString());
        if (hasText(record.getRemark()) && !record.getRemark().contains(base)) {
            return record.getRemark() + " " + base;
        }
        return base;
    }

    private String buildOvertimeSyncRemark(AttendanceRecord record,
                                           com.salary.entity.AttendanceApply apply,
                                           BigDecimal overtimeHours) {
        String base = String.format("审批同步：加班%s小时；当前累计加班%s小时。",
                overtimeHours.stripTrailingZeros().toPlainString(),
                nvl(record.getOvertimeHours(), ZERO).stripTrailingZeros().toPlainString());
        if (hasText(record.getRemark()) && !record.getRemark().contains(base)) {
            return record.getRemark() + " " + base;
        }
        return base;
    }

    private String buildLeaveSyncRemark(AttendanceRecord record,
                                        com.salary.entity.AttendanceApply apply,
                                        BigDecimal actualLeaveDays,
                                        BigDecimal deductedLeaveDays) {
        String leaveLabel = Integer.valueOf(LEAVE_TYPE_SICK).equals(apply.getLeaveType()) ? "病假" : "事假";
        String base = String.format("审批同步：%s%s天，折算请假%s天；当前请假%s天，病假%s天。",
                leaveLabel,
                actualLeaveDays.stripTrailingZeros().toPlainString(),
                deductedLeaveDays.stripTrailingZeros().toPlainString(),
                nvl(record.getLeaveDays(), ZERO).stripTrailingZeros().toPlainString(),
                nvl(record.getSickLeaveDays(), ZERO).stripTrailingZeros().toPlainString());
        if (hasText(record.getRemark()) && !record.getRemark().contains(base)) {
            return record.getRemark() + " " + base;
        }
        return base;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
