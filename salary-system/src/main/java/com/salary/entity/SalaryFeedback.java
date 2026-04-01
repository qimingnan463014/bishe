package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资反馈/投诉实体
 * 对应数据库表：t_salary_feedback
 */
@Data
@TableName("t_salary_feedback")
public class SalaryFeedback {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 类型：1=薪资异议，2=计算错误投诉，3=其他投诉 */
    private Integer feedbackType;

    private Long empId;
    private String empNo;
    private String empName;
    private Long salaryRecordId;
    @TableField("`year_month`")
    private String yearMonth;

    /** 反馈标题 */
    private String title;

    /** 详细反馈内容 */
    private String content;

    /** 附件路径 */
    private String attachment;

    /** 处理状态：0=待处理，1=处理中，2=已解决，3=已驳回 */
    private Integer status;

    /** 管理员/经理回复内容 */
    private String replyContent;

    private Long replyUserId;
    private String replyUserName;
    private LocalDateTime replyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
