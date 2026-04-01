package com.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.salary.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工Mapper接口
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 分页查询员工（关联部门、岗位、经理信息）
     *
     * @param page       分页参数
     * @param empNo      工号（模糊匹配，可空）
     * @param realName   姓名（模糊匹配，可空）
     * @param deptId     部门ID（精确匹配，可空）
     * @param status     状态，可空
     * @param managerId  经理用户ID（经理查本部门时传入，可空）
     * @return 包含部门名、岗位名、经理姓名的员工列表
     */
    Page<Employee> selectPageWithDetails(Page<Employee> page,
                                         @Param("empNo") String empNo,
                                         @Param("realName") String realName,
                                         @Param("deptId") Long deptId,
                                         @Param("status") Integer status,
                                         @Param("managerId") Long managerId);

    /**
     * 根据工号查询员工详情（关联部门、岗位）
     */
    Employee selectByEmpNo(@Param("empNo") String empNo);

    /**
     * 根据userId查询员工（登录后获取员工档案）
     */
    Employee selectByUserId(@Param("userId") Long userId);

    /**
     * 查询指定部门的所有在职员工（经理用，薪资/考勤录入时选人）
     */
    List<Employee> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询某个经理名下的所有在职员工（经理批量算薪用）
     */
    List<Employee> selectByManagerId(@Param("managerId") Long managerId);

    /**
     * 批量插入员工（EasyExcel导入用）
     */
    int batchInsert(@Param("list") List<Employee> employeeList);

    /**
     * 统计各部门人数分布
     */
    List<java.util.Map<String, Object>> countByDepartment();

    /**
     * 分页查询部门经理（关联部门、岗位、经理信息）
     *
     * @param page       分页参数
     * @param empNo      工号（模糊匹配，可空）
     * @param realName   姓名（模糊匹配，可空）
     * @param deptId     部门ID（精确匹配，可空）
     * @param status     状态，可空
     * @return 包含部门名、岗位名、经理姓名的经理列表
     */
    Page<Employee> selectPageWithDetailsForManager(Page<Employee> page,
                                                    @Param("empNo") String empNo,
                                                    @Param("realName") String realName,
                                                    @Param("deptId") Long deptId,
                                                    @Param("status") Integer status);
}
