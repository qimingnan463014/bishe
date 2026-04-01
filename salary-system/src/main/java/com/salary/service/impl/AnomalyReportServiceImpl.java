package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.AnomalyReport;
import com.salary.mapper.AnomalyReportMapper;
import com.salary.service.AnomalyReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnomalyReportServiceImpl extends ServiceImpl<AnomalyReportMapper, AnomalyReport> implements AnomalyReportService {

    @Override
    public PageResult<AnomalyReport> page(int current, int size, Integer reportType, Integer status, Long reporterId) {
        Page<AnomalyReport> page = new Page<>(current, size);
        LambdaQueryWrapper<AnomalyReport> qw = new LambdaQueryWrapper<>();
        if (reportType != null) {
            qw.eq(AnomalyReport::getReportType, reportType);
        }
        if (status != null) {
            qw.eq(AnomalyReport::getStatus, status);
        }
        if (reporterId != null) {
            qw.eq(AnomalyReport::getReporterId, reporterId);
        }
        qw.orderByDesc(AnomalyReport::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReport(Long id, Integer status, String processResult, Long processorId) {
        AnomalyReport report = getById(id);
        if (report == null) throw new RuntimeException("该异常报告不存在");
        report.setStatus(status);
        report.setProcessResult(processResult);
        report.setProcessorId(processorId);
        report.setProcessTime(LocalDateTime.now());
        updateById(report);
    }
}
