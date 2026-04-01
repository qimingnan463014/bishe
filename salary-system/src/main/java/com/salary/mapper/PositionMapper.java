package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salary.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位 Mapper
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    /**
     * 查询指定部门的所有启用岗位（下拉框用）
     */
    List<Position> selectByDeptId(@Param("deptId") Long deptId);
}
