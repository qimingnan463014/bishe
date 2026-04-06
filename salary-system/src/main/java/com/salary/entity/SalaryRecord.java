package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 月度薪资核算记录实体
 * 对应数据库表：t_salary_record
 * 核心计算结果表：应发/扣款/实发完整明细
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_salary_record")
public class SalaryRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 薪资所属年月，格式：YYYY-MM */
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

    /** 银行账户号（冗余，工资条用） */
    private String bankAccount;

    /** 经理用户ID（冗余） */
    private Long managerId;

    /** 经理账号（冗余） */
    private String managerNo;

    /** 经理姓名（冗余） */
    private String managerName;

    /** 关联考勤记录ID */
    private Long attendanceId;

    // ============================
    // 应发项（加项）
    // ============================

    /** 基本工资（元） */
    private BigDecimal baseSalary;

    /** 加班工资（元） */
    private BigDecimal overtimePay;

    /** 绩效奖金（元）= 绩效工资基数 × 绩效系数 */
    private BigDecimal perfBonus;

    /** 全勤奖（元），当月无迟到/旷工/请假时发放 */
    private BigDecimal fullAttendBonus;

    /**
     * 津贴/补助合计（元）
     * 包含：交通补贴、餐补、通讯补贴等
     */
    private BigDecimal allowance;

    /** 其他应发收入（元），如年终奖分摊等 */
    private BigDecimal otherIncome;

    /** 应发工资合计（元）= 所有加项之和 */
    private BigDecimal grossSalary;

    // ============================
    // 扣款项（减项）
    // ============================

    /**
     * 个人缴纳社保费用（元）
     * 含：养老+医疗+失业+公积金个人部分
     * 比例参考：养老8% + 医疗2% + 失业0.3% + 公积金12%
     */
    private BigDecimal socialSecurityEmp;

    /**
     * 考勤扣款（元）
     * = 迟到扣款 + 旷工扣款 + 请假扣款
     */
    private BigDecimal attendDeduct;

    /** 其他扣款（元），如借款还款、罚款等 */
    private BigDecimal otherDeduct;

    /**
     * 个人所得税（元）
     * 采用跨月累计预扣法（累计预扣预缴税额-上月累计已缴税额）
     */
    private BigDecimal incomeTax;

    /** 扣款合计（元）= 社保 + 考勤扣 + 其他扣 + 个税 */
    private BigDecimal totalDeduct;

    /** 实发工资（元）= 应发合计 - 扣款合计 */
    private BigDecimal netSalary;

    // ============================
    // 状态
    // ============================

    /**
     * 核算状态：1=草稿，2=待审核，3=已审核，4=已发放，5=已驳回
     * 流转路径：草稿→待审核→已审核→已发放
     */
    private Integer calcStatus;

    /** 核算登记日期 */
    private LocalDate recordDate;

    /** 实际薪资发放日期 */
    private LocalDate payDate;

    /** 发放文件/回单附件路径 */
    private String issueFile;

    /** 工资条是否已向员工/经理发布：0=未发布，1=已发布 */
    private Integer slipPublished;

    /** 工资条发布时间 */
    private LocalDateTime slipPublishTime;

    /** 备注说明 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
