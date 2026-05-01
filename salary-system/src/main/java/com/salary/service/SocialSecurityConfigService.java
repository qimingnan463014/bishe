package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SocialSecurityConfig;

import java.math.BigDecimal;
import java.time.YearMonth;

public interface SocialSecurityConfigService extends IService<SocialSecurityConfig> {
    PageResult<SocialSecurityConfig> page(int current, int size, String configName);
    void addConfig(SocialSecurityConfig config);
    void updateConfig(SocialSecurityConfig config);
    SocialSecurityConfig getCurrentPersonalConfig();
    SocialSecurityConfig resolvePersonalConfigForMonth(YearMonth yearMonth);
    SocialSecurityConfig applyPersonalConfig(String effectiveMonth,
                                            BigDecimal pensionRate,
                                            BigDecimal medicalRate,
                                            BigDecimal unemploymentRate,
                                            BigDecimal fundRate);
}
