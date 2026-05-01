package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.entity.Position;

import java.util.List;

public interface PositionService extends IService<Position> {

    List<Position> listByDeptId(Long deptId);

    void syncByDept(Long deptId, List<Position> positions);

    Position ensureManagerPosition(Long deptId);
}
