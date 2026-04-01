package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.Performance;

/**
 * 绩效评分Service接口
 */
public interface PerformanceService extends IService<Performance> {

    /** 分页查询绩效记录 */
    PageResult<Performance> page(int current, int size,
                                  String yearMonth, String empNo,
                                  Long deptId, Long managerId);

    /**
     * 经理录入/修改绩效评分（含自动计算总分、评级、联动绩效奖金）
     * 同一员工同一月份只允许一条记录（幂等）
     */
    void saveOrUpdateScore(Performance performance, Long managerId);

    /**
     * 新增绩效（saveOrUpdateScore 内部调用，或直接调用）
     */
    Performance addPerformance(Performance perf, Long managerId);

    /**
     * 修改绩效评分（重新计算总分+评级+联动薪资）
     */
    void updatePerformance(Performance perf);

    /**
     * 确认绩效评分（状态：已提交→已确认，确认后不可修改）
     */
    void confirm(Long id);

    /** 查询绩效详情 */
    Performance getDetail(Long id);

    /** 员工查询自己的绩效历史 */
    PageResult<Performance> getMyPerformance(int current, int size, Long empId);
}
