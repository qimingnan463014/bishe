package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.AnomalyReport;

public interface AnomalyReportService extends IService<AnomalyReport> {
    PageResult<AnomalyReport> page(int current, int size, Integer reportType, Integer status, Long reporterId, Long empId);
    void processReport(Long id, Integer status, String processResult, Long processorId);
}
