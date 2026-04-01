package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.TaxAccumulate;
import com.salary.mapper.TaxAccumulateMapper;
import com.salary.service.TaxAccumulateService;
import org.springframework.stereotype.Service;

@Service
public class TaxAccumulateServiceImpl extends ServiceImpl<TaxAccumulateMapper, TaxAccumulate> implements TaxAccumulateService {

    @Override
    public PageResult<TaxAccumulate> page(int current, int size, String taxYear, Long empId) {
        Page<TaxAccumulate> page = new Page<>(current, size);
        LambdaQueryWrapper<TaxAccumulate> qw = new LambdaQueryWrapper<>();
        if (taxYear != null && !taxYear.isEmpty()) qw.eq(TaxAccumulate::getTaxYear, taxYear);
        if (empId != null) qw.eq(TaxAccumulate::getEmpId, empId);
        qw.orderByDesc(TaxAccumulate::getTaxYear, TaxAccumulate::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }
}
