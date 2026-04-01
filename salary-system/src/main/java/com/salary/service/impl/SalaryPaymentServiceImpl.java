package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.SalaryPayment;
import com.salary.entity.SalaryRecord;
import com.salary.mapper.SalaryPaymentMapper;
import com.salary.service.SalaryPaymentService;
import com.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SalaryPaymentServiceImpl extends ServiceImpl<SalaryPaymentMapper, SalaryPayment> implements SalaryPaymentService {

    private final SalaryService salaryService;

    @Override
    public PageResult<SalaryPayment> page(int current, int size, String yearMonth, Long empId, Integer payStatus) {
        Page<SalaryPayment> page = new Page<>(current, size);
        LambdaQueryWrapper<SalaryPayment> qw = new LambdaQueryWrapper<>();
        if (yearMonth != null) qw.eq(SalaryPayment::getYearMonth, yearMonth);
        if (empId != null) qw.eq(SalaryPayment::getEmpId, empId);
        if (payStatus != null) qw.eq(SalaryPayment::getPayStatus, payStatus);
        qw.orderByDesc(SalaryPayment::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executePayment(Long id, Long operatorId, String operatorName) {
        SalaryPayment payment = getById(id);
        if (payment == null) throw new RuntimeException("付款单不存在");
        if (payment.getPayStatus() == 2) throw new RuntimeException("该薪资已发放，勿重复操作");

        // 模拟打款逻辑...
        payment.setPayStatus(2); // 已发放
        payment.setOperatorId(operatorId);
        payment.setOperatorName(operatorName);
        payment.setPayDate(LocalDate.now());
        updateById(payment);

        // 同步更新关联的账单状态
        SalaryRecord record = salaryService.getById(payment.getSalaryRecordId());
        if (record != null) {
            record.setCalcStatus(4); // 4=已发放
            record.setPayDate(LocalDate.now());
            salaryService.updateById(record);
        }
    }
}
