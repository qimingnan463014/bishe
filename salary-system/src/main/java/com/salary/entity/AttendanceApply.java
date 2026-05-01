package com.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.math.BigDecimal;
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
    private String empNo;
    private String empName;
    private Long deptId;
    private Integer applyType; // 1=补签，2=销假，3=加班申请，4=考勤异议
    private Integer leaveType; // 1=事假，2=病假
    private Integer signType; // 1=上午补签，2=下午补签，3=全天补签
    private BigDecimal leaveDays; // 申请天数（实际天数）
    private BigDecimal overtimeHours; // 加班时长（小时）
    private LocalDate applyDate;
    private String reason;
    private String proof;
    private Integer status; // 0=待审批，1=已通过，2=已拒绝
    private Long reviewUserId;
    private String reviewUserName;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
