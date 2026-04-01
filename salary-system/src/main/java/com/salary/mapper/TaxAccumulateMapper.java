package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.TaxAccumulate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 个税累计预扣Mapper接口
 */
@Mapper
public interface TaxAccumulateMapper extends BaseMapper<TaxAccumulate> {

    /**
     * 查询员工上个月的累计个税数据（用于本月计算）
     *
     * @param empId    员工ID
     * @param taxYear  税务年度（如：2026）
     * @param prevMonth 上个月的年月（如：2026-02）
     * @return 上月累计记录，1月时返回null（重置）
     */
    TaxAccumulate selectPrevMonthAccumulate(@Param("empId") Long empId,
                                             @Param("taxYear") String taxYear,
                                             @Param("prevMonth") String prevMonth);

    /**
     * 查询员工当年累计数据（年度汇总展示用）
     */
    TaxAccumulate selectLatestByEmpAndYear(@Param("empId") Long empId,
                                            @Param("taxYear") String taxYear);
}
