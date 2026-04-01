package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤规则配置实体
 * 对应数据库表：t_attendance_rule
 */
@Data
@TableName("t_attendance_rule")
public class AttendanceRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 月标准工作天数（日薪计算基数，默认22天） */
    private Integer workDays;

    /** 每日标准工作小时数 */
    private BigDecimal workHours;

    /** 迟到每次扣款金额（元，默认50元） */
    private BigDecimal lateDeductPerTime;

    /** 迟到认定阈值（分钟），超过此时长按旷工半天处理 */
    private Integer lateThresholdMin;

    /** 旷工每天扣款额（元），0=按日薪全额 */
    private BigDecimal absentDeductPerDay;

    /** 事假扣款比例（1.0=全额扣） */
    private BigDecimal leaveDeductRatio;

    /** 病假扣款比例（0.8=扣80%） */
    private BigDecimal sickLeaveDeductRatio;

    /** 年度带薪年假天数 */
    private Integer annualLeaveDays;

    /** 是否为当前生效规则 */
    private Integer isActive;

    /** 规则生效日期 */
    private LocalDate effectiveDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
