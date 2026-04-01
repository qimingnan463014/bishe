package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    @Select("select * from t_sys_config where config_key = #{key}")
    SysConfig selectByKey(String key);
}
