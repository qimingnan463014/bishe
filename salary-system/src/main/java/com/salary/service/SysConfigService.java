package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    /** 根据 key 获取配置 */
    SysConfig getByConfigKey(String key);
    /** 保存或更新配置 (根据 key) */
    void saveOrUpdateByKey(String key, String value);
}
