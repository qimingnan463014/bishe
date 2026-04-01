package com.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_attendance_apply")
@ApiModel("考勤申请表")
public class AttendanceApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long attendanceId; // 关联的考勤记录ID
    private Long empId;
    private Integer applyType; // 1=补签，2=销假，3=加班申请，4=考勤异议
    private LocalDate applyDate;
    private String reason;
    private String proof;
    private Integer status; // 0=待审批，1=已通过，2=已拒绝
    private Long reviewUserId;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
