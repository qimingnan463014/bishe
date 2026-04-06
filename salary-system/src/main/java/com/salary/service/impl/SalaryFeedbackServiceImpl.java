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
import java.util.List;

@Service
public class SalaryFeedbackServiceImpl extends ServiceImpl<SalaryFeedbackMapper, SalaryFeedback>
        implements SalaryFeedbackService {

    @Override
    public PageResult<SalaryFeedback> page(int current, int size, List<Integer> types, Integer status, Long empId) {
        if (empId == null) {
            return page(current, size, types, status, (List<Long>) null);
        }
        return page(current, size, types, status, java.util.Collections.singletonList(empId));
    }

    @Override
    public PageResult<SalaryFeedback> page(int current, int size, List<Integer> types, Integer status, List<Long> empIds) {
        Page<SalaryFeedback> page = new Page<>(current, size);
        LambdaQueryWrapper<SalaryFeedback> queryWrapper = new LambdaQueryWrapper<>();
        if (types != null && !types.isEmpty()) {
            queryWrapper.in(SalaryFeedback::getFeedbackType, types);
        }
        if (status != null) {
            queryWrapper.eq(SalaryFeedback::getStatus, status);
        }
        if (empIds != null) {
            if (empIds.isEmpty()) {
                queryWrapper.eq(SalaryFeedback::getEmpId, -1L);
            } else {
                queryWrapper.in(SalaryFeedback::getEmpId, empIds);
            }
        }
        queryWrapper.orderByDesc(SalaryFeedback::getCreateTime);
        this.page(page, queryWrapper);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyFeedback(Long id, Integer status, String replyContent, Long replyUserId) {
        SalaryFeedback feedback = getById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈记录不存在");
        }
        feedback.setStatus(status);
        feedback.setReplyContent(replyContent);
        feedback.setReplyUserId(replyUserId);
        feedback.setReplyTime(LocalDateTime.now());
        updateById(feedback);
    }
}
