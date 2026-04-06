package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.salary.entity.SalaryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 薪资核算Mapper接口
 */
@Mapper
public interface SalaryRecordMapper extends BaseMapper<SalaryRecord> {

    /**
     * 分页查询薪资核算记录
     *
     * @param page       分页参数
     * @param yearMonth  薪资年月（精确匹配，格式：YYYY-MM，可空）
     * @param empNo      工号（精确匹配，可空）
     * @param realName   员工姓名（模糊匹配，可空）
     * @param deptId     部门ID，可空
     * @param calcStatus 核算状态，可空
     * @param managerId  经理限定：只查自己部门的（可空，null=不限制）
     * @return 分页结果
     */
    Page<SalaryRecord> selectPageWithCondition(Page<SalaryRecord> page,
                                               @Param("yearMonth") String yearMonth,
                                               @Param("empNo") String empNo,
                                               @Param("realName") String realName,
                                               @Param("deptId") Long deptId,
                                               @Param("calcStatus") Integer calcStatus,
                                               @Param("managerId") Long managerId,
                                               @Param("excludeEmpNo") String excludeEmpNo,
                                               @Param("excludeDraft") Boolean excludeDraft);

    /**
     * 查询员工某年月的薪资记录（员工自助查询）
     *
     * @param empId     员工ID
     * @param yearMonth 年月
     */
    SalaryRecord selectByEmpAndMonth(@Param("empId") Long empId, @Param("yearMonth") String yearMonth);

    /**
     * 员工查询自己的薪资历史列表（分页）
     */
    Page<SalaryRecord> selectByEmpId(Page<SalaryRecord> page, @Param("empId") Long empId);

    /**
     * 统计月度实发工资总额趋势（近12个月）
     */
    List<java.util.Map<String, Object>> countMonthlyTrend(@Param("anchorYearMonth") String anchorYearMonth);

    /**
     * 统计当月薪资构成（基本、加班、绩效、补助、扣除）
     */
    java.util.Map<String, Object> sumCurrentMonthStructure(@Param("yearMonth") String yearMonth);

    /**
     * 各部门人均薪资对比
     */
    List<java.util.Map<String, Object>> avgSalaryByDepartment(@Param("yearMonth") String yearMonth);
}
