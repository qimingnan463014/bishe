package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资发放记录实体
 * 对应数据库表：t_salary_payment
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_salary_payment")
public class SalaryPayment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联薪资核算记录ID */
    private Long salaryRecordId;

    private Long empId;
    private String empNo;
    private String empName;
    @TableField("`year_month`")
    private String yearMonth;

    /** 实发金额（元） */
    private BigDecimal netSalary;

    /** 收款银行账户 */
    private String bankAccount;

    /** 收款开户行 */
    private String bankName;

    /** 发放日期 */
    private LocalDate payDate;

    /** 发放方式：1=银行转账，2=现金，3=支票 */
    private Integer payMethod;

    /** 发放状态：1=待发放，2=已发放，3=发放失败 */
    private Integer payStatus;

    /** 操作人（管理员/经理）用户ID */
    private Long operatorId;

    /** 操作人姓名（冗余） */
    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
