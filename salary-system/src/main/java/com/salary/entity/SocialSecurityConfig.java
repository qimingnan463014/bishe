package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社保及公积金缴纳比例配置实体
 * 对应数据库表：t_social_security_config
 */
@Data
@TableName("t_social_security_config")
public class SocialSecurityConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 配置方案名称，如：2026年社保方案 */
    @TableField("config_name")
    private String configName;

    /** 养老保险个人缴纳比例（8%→0.0800） */
    @TableField("pension_rate")
    private BigDecimal pensionEmpRatio;

    /** 养老保险单位缴纳比例（当前表未单独存储） */
    @TableField(exist = false)
    private BigDecimal pensionCompRatio;

    /** 医疗保险个人缴纳比例（2%） */
    @TableField("medical_rate")
    private BigDecimal medicalEmpRatio;

    /** 医疗保险单位缴纳比例（当前表未单独存储） */
    @TableField(exist = false)
    private BigDecimal medicalCompRatio;

    /** 失业保险个人缴纳比例（0.3%） */
    @TableField("unemployment_rate")
    private BigDecimal unemploymentEmpRatio;

    /** 失业保险单位缴纳比例（当前表未单独存储） */
    @TableField(exist = false)
    private BigDecimal unemploymentCompRatio;

    /** 工伤保险单位缴纳比例（个人不缴） */
    @TableField("injury_rate")
    private BigDecimal injuryCompRatio;

    /** 住房公积金个人缴纳比例（12%） */
    @TableField("fund_rate")
    private BigDecimal fundEmpRatio;

    /** 住房公积金单位缴纳比例（当前表未单独存储） */
    @TableField(exist = false)
    private BigDecimal fundCompRatio;

    /** 缴费基数下限（元） */
    @TableField("base_min")
    private BigDecimal calcBaseMin;

    /** 缴费基数上限（元） */
    @TableField("base_max")
    private BigDecimal calcBaseMax;

    /** 是否为当前生效方案：1=是 */
    @TableField("is_active")
    private Integer isActive;

    /** 生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /** 生育保险单位缴纳比例（当前表未在页面使用） */
    @TableField("maternity_rate")
    private BigDecimal maternityCompRatio;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
