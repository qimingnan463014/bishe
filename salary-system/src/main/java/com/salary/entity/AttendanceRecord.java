package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 月度考勤汇总数据实体
 * 对应数据库表：t_attendance_record
 * 经理按月录入，作为薪资计算的核心输入
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_attendance_record")
public class AttendanceRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登记编号（系统自动生成，如：9000000007） */
    private String recordNo;

    /** 所属年月，格式：YYYY-MM */
    @TableField("`year_month`")
    private String yearMonth;

    /** 员工ID */
    private Long empId;

    /** 工号（冗余） */
    private String empNo;

    /** 员工姓名（冗余） */
    private String empName;

    /** 部门ID（冗余） */
    private Long deptId;

    /** 部门名称（冗余） */
    private String deptName;

    /** 实际出勤天数 */
    private Integer attendDays;

    /**
     * 旷工天数（支持0.5天）
     * 扣款逻辑：旷工天数 × (基本工资 / 月标准工作天数)
     */
    private BigDecimal absentDays;

    /**
     * 迟到次数
     * 扣款逻辑：迟到次数 × 每次扣款金额（考勤规则配置）
     */
    private Integer lateTimes;

    /**
     * 早退次数
     * 扣款逻辑：早退次数 × 每次扣款金额（与迟到同口径）
     */
    private Integer earlyLeaveTimes;

    /**
     * 请假天数（事假+病假合计，支持0.5天）
     * 扣款逻辑：请假天数 × 日薪 × 请假扣款比例（考勤规则配置）
     */
    private BigDecimal leaveDays;

    /** 其中病假天数 */
    private BigDecimal sickLeaveDays;

    /** 加班小时数（工作日+节假日合计） */
    private BigDecimal overtimeHours;

    /** 实际出勤总时长（小时） */
    private BigDecimal attendHours;

    /** 系统预算考勤扣款合计（元） */
    private BigDecimal attendDeduct;

    /** 登记日期 */
    private LocalDate recordDate;

    /** 录入经理的用户ID */
    private Long managerId;

    /** 经理账号（冗余） */
    private String managerNo;

    /** 经理姓名（冗余） */
    private String managerName;

    /** 状态：1=正常，2=已申诉调整，3=已锁定 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
