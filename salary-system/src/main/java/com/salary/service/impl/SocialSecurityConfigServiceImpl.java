package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.SocialSecurityConfig;
import com.salary.mapper.SocialSecurityConfigMapper;
import com.salary.service.SocialSecurityConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDate;

@Service
public class SocialSecurityConfigServiceImpl extends ServiceImpl<SocialSecurityConfigMapper, SocialSecurityConfig> implements SocialSecurityConfigService {

    @Override
    public PageResult<SocialSecurityConfig> page(int current, int size, String configName) {
        Page<SocialSecurityConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<SocialSecurityConfig> qw = new LambdaQueryWrapper<>();
        if(StringUtils.hasText(configName)) {
            qw.like(SocialSecurityConfig::getConfigName, configName);
        }
        qw.orderByDesc(SocialSecurityConfig::getIsActive).orderByDesc(SocialSecurityConfig::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addConfig(SocialSecurityConfig config) {
        if(config.getIsActive() != null && config.getIsActive() == 1) {
            // 如果新加的是启用的状态，需要把其他的全置为不启用（这里假设全局只有一套正在生效的）
            LambdaQueryWrapper<SocialSecurityConfig> updateQw = new LambdaQueryWrapper<>();
            updateQw.eq(SocialSecurityConfig::getIsActive, 1);
            SocialSecurityConfig updateEntity = new SocialSecurityConfig();
            updateEntity.setIsActive(0);
            update(updateEntity, updateQw);
        }
        if(config.getEffectiveDate() == null) config.setEffectiveDate(LocalDate.now());
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SocialSecurityConfig config) {
        if(config.getIsActive() != null && config.getIsActive() == 1) {
            LambdaQueryWrapper<SocialSecurityConfig> updateQw = new LambdaQueryWrapper<>();
            updateQw.eq(SocialSecurityConfig::getIsActive, 1).ne(SocialSecurityConfig::getId, config.getId());
            SocialSecurityConfig updateEntity = new SocialSecurityConfig();
            updateEntity.setIsActive(0);
            update(updateEntity, updateQw);
        }
        updateById(config);
    }
}
