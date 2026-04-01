package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.entity.SysConfig;
import com.salary.mapper.SysConfigMapper;
import com.salary.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> 
        implements SysConfigService {

    @Override
    public SysConfig getByConfigKey(String key) {
        return this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
    }

    @Override
    public void saveOrUpdateByKey(String key, String value) {
        SysConfig exists = this.getByConfigKey(key);
        if (exists != null) {
            exists.setConfigValue(value);
            this.updateById(exists);
        } else {
            SysConfig nc = new SysConfig();
            nc.setConfigKey(key);
            nc.setConfigValue(value);
            this.save(nc);
        }
    }
}
