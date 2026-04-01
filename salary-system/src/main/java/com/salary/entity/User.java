package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户实体（RBAC账户）
 * 对应数据库表：t_user
 * 角色：1=超级管理员，2=部门经理，3=普通员工
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录用户名/工号 */
    private String username;

    /** 密码（BCrypt加密存储，查询时排除） */
    @TableField(select = false)
    private String password;

    /** 后台管理展示用的当前密码 */
    @TableField(exist = false)
    private String displayPassword;

    /**
     * 角色：1=超级管理员，2=部门经理，3=普通员工
     * RBAC权限控制的核心字段
     */
    private Integer role;

    /** 真实姓名（冗余字段，显示用） */
    private String realName;

    /** 头像文件路径 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
