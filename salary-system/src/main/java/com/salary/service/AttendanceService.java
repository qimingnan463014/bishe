package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.AttendanceApply;
import com.salary.entity.AttendanceRecord;

/**
 * 考勤Service接口
 */
public interface AttendanceService extends IService<AttendanceRecord> {

    /**
     * 分页查询考勤记录
     */
    PageResult<AttendanceRecord> page(int current, int size,
                                       String recordNo, String yearMonth,
                                       String empNo, Long deptId, Long managerId);

    /**
     * 经理录入考勤（自动生成登记编号，校验唯一性）
     */
    void addRecord(AttendanceRecord record, Long managerId);

    /**
     * 修改考勤记录
     */
    void updateRecord(AttendanceRecord record);

    /**
     * 删除考勤记录（已锁定/已关联薪资禁止删除）
     */
    void deleteRecord(Long id);

    /**
     * 查询考勤详情
     */
    AttendanceRecord getDetail(Long id);

    /**
     * 员工查询自己的考勤记录
     */
    PageResult<AttendanceRecord> getMyAttendance(int current, int size, Long empId, String yearMonth);

    /**
     * Import clock-in Excel, auto-analyze and upsert attendance records.
     * Returns per-employee summary (including error messages for bad rows).
     */
    java.util.List<com.salary.dto.AttendanceSummaryDTO> importClockExcel(
            org.springframework.web.multipart.MultipartFile file,
            String yearMonth,
            Long managerId);

    /**
     * 获取指定月份的考勤状态分布统计
     */
    java.util.Map<String, Object> getAttendanceStatus(String yearMonth);

    /**
     * 将审批通过的请假申请同步到当月考勤记录。
     */
    AttendanceRecord syncApprovedLeaveToAttendance(AttendanceApply apply, Long reviewerId, String reviewerName);
}
