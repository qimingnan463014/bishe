package com.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_anomaly_report")
@ApiModel("异常上报记录")
public class AnomalyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer reportType; // 1=考勤异常，2=薪资计算异常，3=系统数据异常
    private Long reporterId;    // 上报人（经理）用户ID
    private Long empId;         // 涉及员工ID
    @com.baomidou.mybatisplus.annotation.TableField("`year_month`")
    private String yearMonth;   // 涉及年月
    private String title;
    private String description;
    private Integer status;     // 处理状态：0=待处理，1=处理中，2=已解决
    private String processResult; // 管理员处理结果
    private Long processorId;   // 处理人（管理员）用户ID
    private LocalDateTime processTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
