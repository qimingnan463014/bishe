package com.salary.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 考勤打卡 Excel 导入 DTO
 * <p>
 * 每一行代表一名员工某天的打卡记录
 * Excel 格式示例：
 * | 工号 | 姓名 | 日期       | 上午打卡时间 | 下午打卡时间 |
 * | 001  | 张三 | 2026-03-01 | 08:55       | 18:30       |
 */
@Data
public class AttendanceClockDTO {

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String empName;

    /**
     * 日期，格式：yyyy-MM-dd
     */
    @ExcelProperty("日期")
    private String workDate;

    /**
     * 上午打卡时间，格式：HH:mm
     * 规定上班时间 09:00，晚于则记迟到
     */
    @ExcelProperty("上午打卡时间")
    private String morningClockIn;

    /**
     * 下午打卡时间，格式：HH:mm
     * 规定下班时间 18:00，早于则记早退
     * 晚于 19:00 则计算加班时长（整小时向下取整）
     */
    @ExcelProperty("下午打卡时间")
    private String afternoonClockOut;
}
