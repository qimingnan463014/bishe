package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.salary.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门Mapper接口
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 分页查询部门（关联查询岗位数量）
     *
     * @param page    分页参数
     * @param deptName 部门名称（模糊匹配，可空）
     * @param status   状态，可空
     * @return 分页结果
     */
    Page<Department> selectPageWithCount(Page<Department> page,
                                         @Param("deptName") String deptName,
                                         @Param("status") Integer status);

    /**
     * 查询所有启用状态的部门（下拉框用）
     */
    List<Department> selectAllEnabled();
}
