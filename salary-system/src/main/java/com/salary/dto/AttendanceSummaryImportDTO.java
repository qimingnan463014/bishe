package com.salary.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 考勤汇总 Excel 导入 DTO
 * 直接对应管理端列表中的月度汇总列
 */
@Data
public class AttendanceSummaryImportDTO {

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String empName;

    @ExcelProperty("出勤天数")
    private String attendDays;

    @ExcelProperty("旷工天数")
    private String absentDays;

    @ExcelProperty("迟到天数")
    private String lateTimes;

    @ExcelProperty("早退次数")
    private String earlyLeaveTimes;

    @ExcelProperty("请假天数")
    private String leaveDays;

    @ExcelProperty("加班小时")
    private String overtimeHours;
}
