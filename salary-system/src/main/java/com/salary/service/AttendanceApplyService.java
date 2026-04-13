package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.AttendanceApply;

public interface AttendanceApplyService extends IService<AttendanceApply> {
    PageResult<AttendanceApply> page(int current, int size, Long empId, Integer status, Long managerId);
    void reviewApply(Long id, Integer status, String comment, Long managerId, String reviewerName);
}
