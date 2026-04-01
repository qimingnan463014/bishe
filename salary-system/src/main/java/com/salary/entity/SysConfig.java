package com.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置实体
 * 用于存储网站“轮播图”、“关于我们”等动态内容
 */
@Data
@TableName("t_sys_config")
public class SysConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配置键 (例如: website_carousel, about_us) */
    private String configKey;

    /** 配置值 (JSON 字符串或长文本) */
    private String configValue;
}
