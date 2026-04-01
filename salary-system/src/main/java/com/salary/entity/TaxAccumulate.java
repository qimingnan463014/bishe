package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个税及社保年度累计预扣记录实体
 * 对应数据库表：t_tax_accumulate
 * 支持跨月累计预扣预缴法（IIT累进税率）
 * 每年1月重置，全年累计计算
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_tax_accumulate")
public class TaxAccumulate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long empId;

    /** 税务年度（如：2026），每年独立累计 */
    private String taxYear;

    /** 截至当月，格式：YYYY-MM */
    @TableField("`year_month`")
    private String yearMonth;

    // ============================
    // 本月数据
    // ============================

    /**
     * 本月应纳税所得额（元）
     * = 应发工资 - 社保个人扣 - 5000起征点 - 专项附加扣除
     */
    private BigDecimal monthTaxableIncome;

    /** 本月预缴个税（元） */
    private BigDecimal monthTax;

    /** 本月个人缴纳社保（不含公积金，元） */
    private BigDecimal monthSocialSecurity;

    /** 本月个人公积金（元） */
    private BigDecimal monthFund;

    /** 本月专项附加扣除（元）：子女教育+继续教育+租房+房贷+赡养老人 */
    private BigDecimal monthSpecialDeduct;

    // ============================
    // 年度累计数据（用于下月计算）
    // ============================

    /** 年度累计应纳税所得额（元） */
    private BigDecimal accumTaxableIncome;

    /** 年度累计已预缴个税（元） */
    private BigDecimal accumTax;

    /** 年度累计应发工资总额（元） */
    private BigDecimal accumGross;

    /** 年度累计个人社保（元） */
    private BigDecimal accumSocialSecurity;

    /** 年度累计专项附加扣除（元） */
    private BigDecimal accumSpecialDeduct;

    /** 关联薪资记录ID */
    private Long salaryRecordId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
