package com.salary.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告实体
 * 对应数据库表：t_announcement
 */
@Data
@TableName("t_announcement")
public class Announcement {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容（富文本） */
    private String content;

    /** 封面图片路径 */
    private String coverImage;

    /** 发布人用户ID */
    private Long pubUserId;

    /** 发布人姓名（冗余） */
    private String pubUserName;

    /** 目标受众：0=全部，1=管理员，2=经理，3=员工 */
    private Integer targetRole;

    /** 是否置顶：1=是，0=否 */
    private Integer isTop;

    /** 状态：1=已发布，0=草稿，2=已撤回 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime pubTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
