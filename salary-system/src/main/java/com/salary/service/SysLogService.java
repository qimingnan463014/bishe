package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SysLog;

public interface SysLogService extends IService<SysLog> {

    PageResult<SysLog> page(int current, int size, String username, String module);

    void recordOperation(String username, Integer role, String module, String action);
}
