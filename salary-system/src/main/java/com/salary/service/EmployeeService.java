package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.Employee;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 员工Service接口
 */
public interface EmployeeService extends IService<Employee> {

    /**
     * 分页查询员工列表（支持按部门、姓名、工号筛选）
     *
     * @param current   当前页
     * @param size      每页数量
     * @param empNo     工号（可空）
     * @param realName  姓名（可空）
     * @param deptId    部门ID（可空）
     * @param status    状态（可空）
     * @param managerId 经理ID限定（经理只查自己部门，可空=不限）
     */
    PageResult<Employee> page(int current, int size, String empNo, String realName,
                              Long deptId, Integer status, Long managerId);

    /**
     * 新增员工（同时创建登录账号）
     */
    void addEmployee(Employee employee, String initialPassword);

    /**
     * 修改员工信息
     */
    void updateEmployee(Employee employee);

    /**
     * 删除员工（软删除，设置为离职状态）
     */
    void deleteEmployee(Long id);

    /**
     * 批量删除员工
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据ID查询员工详情（含部门、岗位、经理信息）
     */
    Employee getDetail(Long id);

    /**
     * 根据当前登录用户ID获取员工个人档案
     */
    Employee getByCurrentUser(Long userId);

    /**
     * EasyExcel 批量导入员工
     */
    void importByExcel(MultipartFile file);

    /**
     * EasyExcel 导出员工列表
     */
    void exportToExcel(HttpServletResponse response, String empNo, String realName, Long deptId);

    /**
     * 获取部门人数分布统计
     */
    List<java.util.Map<String, Object>> getDeptDistribution();

    /**
     * 分页查询部门经理列表（只显示 role=2 的经理）
     *
     * @param current   当前页
     * @param size      每页数量
     * @param empNo     工号（可空）
     * @param realName  姓名（可空）
     * @param deptId    部门ID（可空）
     * @param status    状态（可空）
     */
    PageResult<Employee> pageManager(int current, int size, String empNo, String realName,
                                     Long deptId, Integer status);
}
