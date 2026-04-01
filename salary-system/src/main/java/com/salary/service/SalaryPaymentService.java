package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SalaryPayment;

public interface SalaryPaymentService extends IService<SalaryPayment> {
    PageResult<SalaryPayment> page(int current, int size, String yearMonth, Long empId, Integer payStatus);
    void executePayment(Long id, Long operatorId, String operatorName);
}
