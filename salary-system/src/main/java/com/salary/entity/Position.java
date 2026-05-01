package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位信息实体
 * 对应数据库表：t_position
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_position")
public class Position {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属部门ID */
    private Long deptId;

    /** 岗位名称，如：后端开发工程师、UI设计师 */
    private String positionName;

    /** 该岗位薪资范围下限（元） */
    private BigDecimal salaryMin;

    /** 该岗位薪资范围上限（元） */
    private BigDecimal salaryMax;

    /** 岗位职责描述 */
    private String description;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** 是否为系统保留的经理岗位：1=是，0=否 */
    private Integer isManagerPosition;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
