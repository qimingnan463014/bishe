package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.common.PageResult;
import com.salary.entity.SalaryRecord;

import java.util.List;

/**
 * 薪资Service接口
 */
public interface SalaryService extends IService<SalaryRecord> {

    /** 分页查询薪资核算列表 */
    PageResult<SalaryRecord> page(int current, int size,
                                   String yearMonth, String empNo, String realName,
                                   Long deptId, Integer calcStatus, Long managerId,
                                   String excludeEmpNo, Boolean excludeDraft);

    /** 为指定员工的指定月份自动计算薪资（核心业务） */
    SalaryRecord calculateSalary(Long empId, String yearMonth);

    /** 批量计算某月所有员工薪资（一键算薪） */
    void batchCalculate(String yearMonth, Long deptId);

    /** 发放薪资（状态→已发放，生成发放记录），仅内部调用 */
    void issueSalary(Long salaryRecordId, Long operatorId);

    /** 批量发放薪资（经理操作，状态须为已审核） */
    void issueBatch(List<Long> salaryRecordIds, Long operatorId);

    // ====================================================
    //  ★ 状态流转：发布（经理→员工可见）
    // ====================================================

    /**
     * 一键发布薪资：草稿(1) → 已发布(2)，员工端即可查询
     *
     * @param salaryId 薪资记录ID
     */
    void publishSalary(Long salaryId);

    /**
     * 批量一键发布
     */
    void publishBatch(List<Long> ids);

    // ====================================================
    //  ★ 状态流转：二审（仅管理员 Admin 可操作）
    // ====================================================

    /**
     * 二审通过薪资：已发布待审核(2) → 已审核(3)
     */
    void auditSalary(Long salaryId, Integer role);

    /**
     * 批量二审通过
     */
    void auditBatch(List<Long> ids, Integer role);


    // ====================================================
    //  ★ 状态流转：支付（仅管理员 Admin 可操作）
    // ====================================================

    /**
     * 薪资支付：已审核(3) → 已发放(4)
     *
     * @param salaryId   薪资记录ID
     * @param operatorId 操作人用户ID
     * @param role       操作人角色（必须为1=Admin，否则抛异常）
     */
    void paySalary(Long salaryId, Long operatorId, Integer role);

    /**
     * 批量支付（仅管理员）
     */
    void payBatch(List<Long> ids, Long operatorId, Integer role);

    /** 工资条发布：已发放(4) -> 仅开放给员工/经理查看，不改变 calcStatus */
    void publishSalarySlip(Long salaryId, Long operatorId, String operatorName);

    /** 批量发布工资条 */
    void publishSalarySlips(List<Long> ids, Long operatorId, String operatorName);

    /** 查询薪资详情 */
    SalaryRecord getDetail(Long id);

    /** 员工查询自己的薪资历史（分页） */
    PageResult<SalaryRecord> getMyHistory(int current, int size, Long empId);

    /** 根据员工ID和年月查询薪资单 */
    SalaryRecord getByEmpAndMonth(Long empId, String yearMonth);

    /** 手动修改薪资记录 */
    void manualUpdate(SalaryRecord record);

    /** 更新薪资发放附件 */
    void updateIssueFile(Long salaryId, String issueFile, Integer role);

    /** 获取月度总薪资走势（以指定月份为右边界的近12个月） */
    List<java.util.Map<String, Object>> getMonthlyTrend(String yearMonth);

    /** 获取指定月份的薪资构成（基本、加班、绩效、补助、扣减） */
    java.util.Map<String, Object> getSalaryStructure(String yearMonth, String managerNo, String excludeEmpNo);

    /** 获取指定月份的各部门平均薪资对比 */
    List<java.util.Map<String, Object>> getDeptAvgSalary(String yearMonth);
}
