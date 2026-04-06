package com.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sys_log")
public class SysLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String module;

    private String action;

    @TableField("create_time")
    private LocalDateTime createTime;
}
