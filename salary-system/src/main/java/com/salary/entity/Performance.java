package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度绩效评分实体
 * 对应数据库表：t_performance
 * 经理每月评分，绩效结果直接决定绩效奖金金额
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_performance")
public class Performance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考核年月，格式：YYYY-MM */
    @TableField("`year_month`")
    private String yearMonth;

    /** 被考核员工ID */
    private Long empId;

    /** 工号（冗余） */
    private String empNo;

    /** 员工姓名（冗余） */
    private String empName;

    /** 部门ID（冗余） */
    private Long deptId;

    /**
     * 工作态度得分（经理手动录入，0-100）
     */
    private BigDecimal workAttitude;

    /**
     * 业务技能得分（经理手动录入，0-100）
     */
    private BigDecimal businessSkill;

    /**
     * 工作绩效得分（经理手动录入，0-100）
     */
    private BigDecimal workPerformance;

    /**
     * 奖惩加减分（复用为：员工考勤分 0-30）
     */
    private BigDecimal bonusDeduct;

    @TableField(exist = false)
    private BigDecimal qualifiedBonus;

    @TableField(exist = false)
    private BigDecimal goodBonus;

    @TableField(exist = false)
    private BigDecimal excellentBonus;

    /**
     * 综合总得分（系统自动计算）
     * = workAttitude × 权重 + businessSkill × 权重 + workPerformance × 权重 + bonusDeduct
     * 此项目中默认：三个子项平均分 + 奖惩加减分
     */
    private BigDecimal score;

    /** 绩效等级：优秀/良好/合格/不合格 */
    private String grade;

    /**
     * 绩效奖金金额（元）
     * 由绩效评分自动映射得出，例如：优秀=1500、良好=1200、合格=900、不合格=0
     */
    private BigDecimal perfBonusRatio;

    /** 评分说明 */
    private String evalComment;

    /** 评分经理用户ID */
    private Long managerId;

    /** 评分经理姓名（冗余） */
    private String managerName;

    /** 状态：1=草稿，2=已提交，3=已确认 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
