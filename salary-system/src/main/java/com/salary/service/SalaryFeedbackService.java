package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SalaryFeedback;

import java.util.List;

public interface SalaryFeedbackService extends IService<SalaryFeedback> {
    PageResult<SalaryFeedback> page(int current, int size, List<Integer> types, Integer status, Long empId);
    PageResult<SalaryFeedback> page(int current, int size, List<Integer> types, Integer status, List<Long> empIds);
    void replyFeedback(Long id, Integer status, String replyContent, Long replyUserId);
}
