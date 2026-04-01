package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.Performance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 绩效评分Mapper接口
 */
@Mapper
public interface PerformanceMapper extends BaseMapper<Performance> {

    /**
     * 根据员工ID和年月查询绩效（薪资计算时调用）
     */
    Performance selectByEmpAndMonth(@Param("empId") Long empId, @Param("yearMonth") String yearMonth);
}
