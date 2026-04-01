package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.TaxAccumulate;

public interface TaxAccumulateService extends IService<TaxAccumulate> {
    PageResult<TaxAccumulate> page(int current, int size, String taxYear, Long empId);
}
