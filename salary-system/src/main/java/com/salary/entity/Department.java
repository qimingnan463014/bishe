package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门信息实体
 * 对应数据库表：t_department
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_department")
public class Department {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 部门名称 */
    private String deptName;

    /** 部门编码（唯一） */
    private String deptCode;

    /**
     * 该部门工资基数（元）
     * 核心字段：不同部门基数不同，员工实际薪资在此范围内设定
     */
    private BigDecimal baseSalary;

    /**
     * 该部门职位工资（元）
     * 经理工资 = 部门基本工资 + 职位工资
     */
    private BigDecimal positionSalary;

    /** 部门职能描述 */
    private String description;

    /** 排列顺序 */
    private Integer sortOrder;

    /** 部门经理的用户ID */
    private Long managerId;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
