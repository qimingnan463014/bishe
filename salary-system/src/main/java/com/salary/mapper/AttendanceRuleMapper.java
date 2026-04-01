package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.AttendanceRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤规则Mapper
 */
@Mapper
public interface AttendanceRuleMapper extends BaseMapper<AttendanceRule> {
}
