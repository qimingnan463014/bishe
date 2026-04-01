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
    private String configName;

    /** 养老保险个人缴纳比例（8%→0.0800） */
    private BigDecimal pensionEmpRatio;

    /** 养老保险单位缴纳比例（16%） */
    private BigDecimal pensionCompRatio;

    /** 医疗保险个人缴纳比例（2%） */
    private BigDecimal medicalEmpRatio;

    /** 医疗保险单位缴纳比例（8%） */
    private BigDecimal medicalCompRatio;

    /** 失业保险个人缴纳比例（0.5%） */
    private BigDecimal unemploymentEmpRatio;

    /** 失业保险单位缴纳比例（0.5%） */
    private BigDecimal unemploymentCompRatio;

    /** 工伤保险单位缴纳比例（个人不缴） */
    private BigDecimal injuryCompRatio;

    /** 住房公积金个人缴纳比例（5%-12%） */
    private BigDecimal fundEmpRatio;

    /** 住房公积金单位缴纳比例 */
    private BigDecimal fundCompRatio;

    /** 缴费基数下限（元） */
    private BigDecimal calcBaseMin;

    /** 缴费基数上限（元） */
    private BigDecimal calcBaseMax;

    /** 是否为当前生效方案：1=是 */
    private Integer isActive;

    /** 生效日期 */
    private LocalDate effectiveDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
