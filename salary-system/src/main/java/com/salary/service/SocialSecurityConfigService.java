package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SocialSecurityConfig;

public interface SocialSecurityConfigService extends IService<SocialSecurityConfig> {
    PageResult<SocialSecurityConfig> page(int current, int size, String configName);
    void addConfig(SocialSecurityConfig config);
    void updateConfig(SocialSecurityConfig config);
}
