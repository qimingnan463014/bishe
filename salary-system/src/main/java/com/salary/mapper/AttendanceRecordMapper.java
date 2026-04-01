package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.salary.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 考勤记录Mapper接口
 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    /**
     * 分页查询考勤记录
     *
     * @param page      分页参数
     * @param recordNo  登记编号（精确匹配，可空）
     * @param yearMonth 年月（精确匹配，可空）
     * @param empNo     工号（精确匹配，可空）
     * @param deptId    部门ID（可空）
     * @param managerId 经理限定（可空，null=不限）
     */
    Page<AttendanceRecord> selectPageWithCondition(Page<AttendanceRecord> page,
                                                   @Param("recordNo") String recordNo,
                                                   @Param("yearMonth") String yearMonth,
                                                   @Param("empNo") String empNo,
                                                   @Param("deptId") Long deptId,
                                                   @Param("managerId") Long managerId);

    /**
     * 根据员工ID和年月查询（用于薪资计算前读取考勤）
     */
    AttendanceRecord selectByEmpAndMonth(@Param("empId") Long empId,
                                         @Param("yearMonth") String yearMonth);

    /**
     * 统计指定月份的出勤状态分布（考勤人次）
     */
    java.util.Map<String, Object> countAttendanceStatus(@Param("yearMonth") String yearMonth);
}
