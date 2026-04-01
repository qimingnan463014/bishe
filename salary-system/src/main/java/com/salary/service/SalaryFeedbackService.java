package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SalaryFeedback;

public interface SalaryFeedbackService extends IService<SalaryFeedback> {
    PageResult<SalaryFeedback> page(int current, int size, Integer type, Integer status, Long empId);
    void replyFeedback(Long id, Integer status, String replyContent, Long replyUserId);
}
