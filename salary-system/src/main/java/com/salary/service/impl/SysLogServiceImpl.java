package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.SysLog;
import com.salary.mapper.SysLogMapper;
import com.salary.service.SysLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public PageResult<SysLog> page(int current, int size, String username, String module) {
        Page<SysLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysLog> queryWrapper = new LambdaQueryWrapper<SysLog>()
                .like(StringUtils.hasText(username), SysLog::getUsername, username)
                .like(StringUtils.hasText(module), SysLog::getModule, module)
                .orderByDesc(SysLog::getCreateTime)
                .orderByDesc(SysLog::getId);
        return PageResult.of(page(page, queryWrapper));
    }

    @Override
    public void recordOperation(String username, Integer role, String module, String action) {
        if (!StringUtils.hasText(action)) {
            return;
        }
        SysLog sysLog = new SysLog();
        sysLog.setUsername(StringUtils.hasText(username) ? username : "unknown");
        sysLog.setModule(StringUtils.hasText(module) ? module : "系统管理");
        sysLog.setAction(roleLabel(role) + action.trim());
        save(sysLog);
    }

    private String roleLabel(Integer role) {
        if (role == null) {
            return "用户";
        }
        switch (role) {
            case 1:
                return "管理员";
            case 2:
                return "部门经理";
            case 3:
                return "员工";
            default:
                return "用户";
        }
    }
}
