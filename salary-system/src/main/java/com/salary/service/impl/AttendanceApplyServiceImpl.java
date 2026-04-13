package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.AttendanceApply;
import com.salary.entity.AttendanceRecord;
import com.salary.entity.Employee;
import com.salary.mapper.AttendanceApplyMapper;
import com.salary.mapper.EmployeeMapper;
import com.salary.service.AttendanceApplyService;
import com.salary.service.AttendanceService;
import com.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceApplyServiceImpl extends ServiceImpl<AttendanceApplyMapper, AttendanceApply> implements AttendanceApplyService {

    private final EmployeeMapper employeeMapper;
    private final AttendanceService attendanceService;
    private final SalaryService salaryService;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public PageResult<AttendanceApply> page(int current, int size, Long empId, Integer status, Long managerId) {
        Page<AttendanceApply> page = new Page<>(current, size);
        LambdaQueryWrapper<AttendanceApply> qw = new LambdaQueryWrapper<>();
        if (empId != null) qw.eq(AttendanceApply::getEmpId, empId);
        if (status != null) qw.eq(AttendanceApply::getStatus, status);
        if (managerId != null) {
            List<Long> empIds = employeeMapper.selectList(new LambdaQueryWrapper<Employee>().eq(Employee::getManagerId, managerId))
                    .stream().map(Employee::getId).collect(Collectors.toList());
            if (empIds.isEmpty()) return PageResult.of(new Page<>());
            qw.in(AttendanceApply::getEmpId, empIds);
        }
        qw.orderByDesc(AttendanceApply::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApply(Long id, Integer status, String comment, Long managerId, String reviewerName) {
        AttendanceApply apply = getById(id);
        if (apply == null) throw new RuntimeException("申请不存在");
        if (apply.getStatus() != 0) throw new RuntimeException("申请已被处理");
        apply.setStatus(status);
        apply.setReviewComment(comment);
        apply.setReviewUserId(managerId);
        apply.setReviewUserName(reviewerName);
        apply.setReviewTime(LocalDateTime.now());
        if (Integer.valueOf(1).equals(status) && Integer.valueOf(2).equals(apply.getApplyType())) {
            AttendanceRecord attendanceRecord = attendanceService.syncApprovedLeaveToAttendance(apply, managerId, reviewerName);
            if (attendanceRecord != null) {
                apply.setAttendanceId(attendanceRecord.getId());
                if (apply.getEmpId() != null && apply.getApplyDate() != null) {
                    salaryService.calculateSalary(apply.getEmpId(), apply.getApplyDate().format(YEAR_MONTH_FMT));
                }
            }
        }
        updateById(apply);
    }
}
