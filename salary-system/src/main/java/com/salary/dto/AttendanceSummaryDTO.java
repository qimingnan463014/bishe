package com.salary.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单个员工某月考勤汇总结果（由打卡明细聚合而来）
 * 用于 AttendanceServiceImpl 中将 Excel 打卡数据 → 考勤记录
 */
@Data
public class AttendanceSummaryDTO {

    private String empNo;
    private String empName;

    /** 格式：YYYY-MM */
    private String yearMonth;

    /** 实际出勤天数（有打卡记录的天数） */
    private int attendDays;

    /** 迟到次数（上午打卡 > 09:00） */
    private int lateTimes;

    /** 早退次数（下午打卡 < 18:00） */
    private int earlyLeaveTimes;

    /** 旷工天数（无任何打卡记录，需人工标注或补充逻辑，Excel导入阶段默认0） */
    private BigDecimal absentDays;

    /** 请假天数（同上，由人工录入或审批记录决定，默认0） */
    private BigDecimal leaveDays;

    /** 加班总小时数（下午打卡 >= 19:00，每满1小时记1小时，向下取整） */
    private BigDecimal overtimeHours;

    /** 出勤总时长（小时），= 出勤天数 × 8 + 加班时长（简化计算） */
    private BigDecimal attendHours;

    /** 解析过程中的异常行说明（如格式错误） */
    private List<String> errorMessages;
}
