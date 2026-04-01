package com.salary.entity;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工档案实体
 * 对应数据库表：t_employee
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_employee")
@ExcelIgnoreUnannotated
public class Employee {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联系统用户ID */
    private Long userId;

    /** 工号（业务唯一标识，如：001、2580） */
    @ExcelProperty("工号")
    private String empNo;

    /** 员工姓名 */
    @ExcelProperty("姓名")
    private String realName;

    /** 性别：1=男，2=女 */
    @ExcelProperty("性别")
    private Integer gender;

    /** 身份证号（18位） */
    @ExcelProperty("身份证号")
    private String idCard;

    /** 手机号码 */
    @ExcelProperty("手机")
    private String phone;

    /** 所属部门ID */
    private Long deptId;

    /** 岗位ID */
    private Long positionId;

    /** 直属经理的用户ID，关联t_user */
    private Long managerId;

    /**
     * 入职日期
     * 关键字段：用于计算工龄，以及个税累计预扣的年度起算
     */
    @ExcelProperty("入职日期")
    private LocalDate hireDate;

    /**
     * 员工个人基本工资（元）
     * 在部门工资基数范围内由管理员设定
     */
    @ExcelProperty("基本工资")
    private BigDecimal baseSalary;

    /** 银行账户号 */
    @ExcelProperty("银行卡号")
    private String bankAccount;

    /** 开户行名称 */
    @ExcelProperty("开户行")
    private String bankName;

    /** 头像文件路径 */
    private String avatar;

    /** 在职状态：1=在职，2=离职，3=试用期 */
    private Integer status;

    /** 备注信息 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ====== 以下为关联查询冗余字段（非数据库字段，查询时填充） ======
    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String positionName;

    @TableField(exist = false)
    private String managerNo;

    @TableField(exist = false)
    private String managerName;

    @TableField(exist = false)
    private String loginAccount;

    @TableField(exist = false)
    private String loginPassword;

    @TableField(exist = false)
    private Integer userRole;

    @TableField(exist = false)
    private Integer role;

    @TableField(exist = false)
    private String bankCard;

    public String getBankCard() {
        return this.bankAccount;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
        if (this.bankAccount == null) {
            this.bankAccount = bankCard;
        }
    }

    @TableField(exist = false)
    private String initialPassword;
}
