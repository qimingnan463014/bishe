package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.SalaryPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资发放Mapper接口
 */
@Mapper
public interface SalaryPaymentMapper extends BaseMapper<SalaryPayment> {
}
