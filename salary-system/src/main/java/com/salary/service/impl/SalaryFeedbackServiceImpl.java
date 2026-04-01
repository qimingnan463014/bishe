package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.SalaryFeedback;
import com.salary.mapper.SalaryFeedbackMapper;
import com.salary.service.SalaryFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SalaryFeedbackServiceImpl extends ServiceImpl<SalaryFeedbackMapper, SalaryFeedback> implements SalaryFeedbackService {

    @Override
    public PageResult<SalaryFeedback> page(int current, int size, Integer type, Integer status, Long empId) {
        Page<SalaryFeedback> page = new Page<>(current, size);
        LambdaQueryWrapper<SalaryFeedback> qw = new LambdaQueryWrapper<>();
        if (type != null) qw.eq(SalaryFeedback::getFeedbackType, type);
        if (status != null) qw.eq(SalaryFeedback::getStatus, status);
        if (empId != null) qw.eq(SalaryFeedback::getEmpId, empId);
        qw.orderByDesc(SalaryFeedback::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyFeedback(Long id, Integer status, String replyContent, Long replyUserId) {
        SalaryFeedback fb = getById(id);
        if (fb == null) throw new RuntimeException("反馈记录不存在");
        fb.setStatus(status);
        fb.setReplyContent(replyContent);
        fb.setReplyUserId(replyUserId);
        fb.setReplyTime(LocalDateTime.now());
        updateById(fb);
    }
}
