package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.Department;
import com.salary.entity.Employee;
import com.salary.entity.Performance;
import com.salary.entity.SalaryRecord;
import com.salary.entity.User;
import com.salary.mapper.*;
import com.salary.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 绩效评分 ServiceImpl
 * <p>
 * 核心逻辑：
 * 1. 经理填写四个子分项（工作态度/业务技能/工作绩效/奖惩加减分）
 * 2. 系统自动计算总得分 = (workAttitude + businessSkill + workPerformance) / 3 + bonusDeduct
 * 3. 根据总得分自动映射评价等级：≥90优秀 / ≥80良好 / ≥60一般 / <60差
 * 4. 自动联动更新该员工当月薪资记录中的绩效奖金字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl extends ServiceImpl<PerformanceMapper, Performance>
        implements PerformanceService {

    // 废弃原有的百分比与评级常量，改用新版管理员前端传值直落金额


    private final PerformanceMapper    performanceMapper;
    private final EmployeeMapper       employeeMapper;
    private final DepartmentMapper     departmentMapper;
    private final SalaryRecordMapper   salaryRecordMapper;
    private final UserMapper           userMapper;

    // ====================================================
    //  分页查询
    // ====================================================

    @Override
    public PageResult<Performance> page(int current, int size,
                                         String yearMonth, String empNo,
                                         Long deptId, Long managerId) {
        Page<Performance> page = new Page<>(current, size);
        LambdaQueryWrapper<Performance> qw = new LambdaQueryWrapper<Performance>()
                .eq(yearMonth != null, Performance::getYearMonth, yearMonth)
                .eq(empNo != null, Performance::getEmpNo, empNo)
                .eq(deptId != null, Performance::getDeptId, deptId)
                .eq(managerId != null, Performance::getManagerId, managerId)
                .orderByDesc(Performance::getYearMonth);
        return PageResult.of(performanceMapper.selectPage(page, qw));
    }

    /**
     * 幂等保存：同一员工同一月份已有记录则更新，否则新增
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateScore(Performance perf, Long managerId) {
        Performance exist = performanceMapper.selectByEmpAndMonth(
                perf.getEmpId(), perf.getYearMonth());
        if (exist != null) {
            perf.setId(exist.getId());
            updatePerformance(perf);
        } else {
            addPerformance(perf, managerId);
        }
    }

    /**
     * 确认绩效评分（已提交→已确认），确认后不可修改
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        Performance perf = getById(id);
        if (perf == null) throw new RuntimeException("绩效记录不存在：id=" + id);
        if (perf.getStatus() != null && perf.getStatus() == 3) {
            throw new RuntimeException("该绩效已确认，不可重复操作");
        }
        Performance update = new Performance();
        update.setId(id);
        update.setStatus(3); // 3=已确认
        updateById(update);
        log.info("绩效已确认：id={} empNo={} yearMonth={}", id, perf.getEmpNo(), perf.getYearMonth());
    }

    /**
     * 员工查询自己的绩效历史（只能查已确认的记录）
     */
    @Override
    public PageResult<Performance> getMyPerformance(int current, int size, Long empId) {
        Page<Performance> page = new Page<>(current, size);
        LambdaQueryWrapper<Performance> qw = new LambdaQueryWrapper<Performance>()
                .eq(Performance::getEmpId, empId)
                .ge(Performance::getStatus, 2) // 已提交及以上才对员工可见
                .orderByDesc(Performance::getYearMonth);
        return PageResult.of(performanceMapper.selectPage(page, qw));
    }

    // ====================================================
    //  ★ 核心：新增绩效评分（自动计算总分+定级+联动薪资）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Performance addPerformance(Performance perf, Long managerId) {
        // 1. 填充员工/经理快照字段，避免前端漏传导致入库失败
        fillSnapshotFields(perf, managerId);

        // 2. 自动计算总分与评级
        autoCalcScoreAndGrade(perf);

        // 3. 持久化
        save(perf);

        // 4. 联动更新当月薪资记录的绩效奖金
        syncPerfBonusToSalary(perf);

        log.info("新增绩效：empNo={} yearMonth={} score={} grade={}",
                perf.getEmpNo(), perf.getYearMonth(), perf.getScore(), perf.getGrade());
        return perf;
    }

    // ====================================================
    //  ★ 核心：修改绩效评分（重新计算并联动）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePerformance(Performance perf) {
        Performance exist = getById(perf.getId());
        if (exist == null) throw new RuntimeException("绩效记录不存在：id=" + perf.getId());

        // 兼容前端仅传部分字段：补齐不可为空的快照字段
        if (perf.getEmpId() == null) perf.setEmpId(exist.getEmpId());
        fillSnapshotFields(perf, exist.getManagerId() != null ? exist.getManagerId() : perf.getManagerId());

        // 重新自动计算总分与评级
        autoCalcScoreAndGrade(perf);

        updateById(perf);

        // 联动更新薪资记录
        syncPerfBonusToSalary(perf);

        log.info("修改绩效：empNo={} yearMonth={} score={} grade={}",
                perf.getEmpNo(), perf.getYearMonth(), perf.getScore(), perf.getGrade());
    }

    @Override
    public Performance getDetail(Long id) {
        return getById(id);
    }

    // ====================================================
    //  自动评级算法（核心）
    // ====================================================

    /**
     * 根据四个子分项累加计算总分与等级
     * 计算公式：
     *   总得分 = 工作态度(10) + 业务技能(10) + 工作绩效(50) + 员工考勤(30)
     */
    private void autoCalcScoreAndGrade(Performance perf) {
        BigDecimal attitude    = nvl(perf.getWorkAttitude());
        BigDecimal skill       = nvl(perf.getBusinessSkill());
        BigDecimal workPerf    = nvl(perf.getWorkPerformance());
        BigDecimal bonusDeduct = nvl(perf.getBonusDeduct());

        BigDecimal totalScore = attitude.add(skill).add(workPerf).add(bonusDeduct).setScale(2, RoundingMode.HALF_UP);
        perf.setScore(totalScore);

        BigDecimal qBonus = perf.getQualifiedBonus() != null ? perf.getQualifiedBonus() : new BigDecimal("900");
        BigDecimal gBonus = perf.getGoodBonus() != null ? perf.getGoodBonus() : new BigDecimal("1200");
        BigDecimal eBonus = perf.getExcellentBonus() != null ? perf.getExcellentBonus() : new BigDecimal("1500");

        // 自动映射评级和绝对金额
        if (totalScore.compareTo(new BigDecimal("86")) >= 0) {
            perf.setGrade("优秀");
            perf.setPerfBonusRatio(eBonus);
        } else if (totalScore.compareTo(new BigDecimal("76")) >= 0) {
            perf.setGrade("良好");
            perf.setPerfBonusRatio(gBonus);
        } else if (totalScore.compareTo(new BigDecimal("61")) >= 0) {
            perf.setGrade("合格");
            perf.setPerfBonusRatio(qBonus);
        } else {
            perf.setGrade("不合格");
            perf.setPerfBonusRatio(BigDecimal.ZERO);
        }

        log.debug("自动评级：score={} → grade={} amount={}",
                totalScore, perf.getGrade(), perf.getPerfBonusRatio());
    }

    // ====================================================
    //  联动更新薪资记录中的绩效奖金
    // ====================================================

    /**
     * 将绩效评分结果同步到当月薪资草稿记录（如果该记录已存在）
     * <p>
     * 当月薪资记录中的 perfBonus 将被更新为：
     *   绩效奖金 = 部门基数 × 20% × 绩效系数（perfBonusRatio）
     */
    private void syncPerfBonusToSalary(Performance perf) {
        // 查询当月薪资记录是否存在（可能还未核算，则不做联动）
        SalaryRecord salaryRecord = salaryRecordMapper.selectByEmpAndMonth(
                perf.getEmpId(), perf.getYearMonth());
        if (salaryRecord == null) {
            log.info("当月薪资记录尚未生成，跳过绩效奖金联动：empId={} yearMonth={}",
                    perf.getEmpId(), perf.getYearMonth());
            return;
        }

        // 读取部门基数
        Employee emp = employeeMapper.selectById(perf.getEmpId());
        if (emp == null) return;
        Department dept = departmentMapper.selectById(emp.getDeptId());
        if (dept == null || dept.getBaseSalary() == null) return;
        // 绩效奖金 = perfBonusRatio 中保存的管理员设置的绝对金额
        BigDecimal perfBonus = nvl(perf.getPerfBonusRatio());

        // 更新薪资记录的绩效奖金字段（同时重算应发和实发）
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryRecord.getId());
        update.setPerfBonus(perfBonus);

        // 重算应发 = 原应发 - 原绩效奖金 + 新绩效奖金
        BigDecimal oldPerfBonus = nvl(salaryRecord.getPerfBonus());
        BigDecimal deltaPerf    = perfBonus.subtract(oldPerfBonus);
        BigDecimal newGross     = nvl(salaryRecord.getGrossSalary()).add(deltaPerf);
        BigDecimal newNet       = nvl(salaryRecord.getNetSalary()).add(deltaPerf);

        update.setGrossSalary(newGross.setScale(2, RoundingMode.HALF_UP));
        update.setNetSalary(newNet.setScale(2, RoundingMode.HALF_UP));

        salaryRecordMapper.updateById(update);

        log.info("联动更新绩效奖金：empNo={} yearMonth={} perfBonus={}→{}",
                perf.getEmpNo(), perf.getYearMonth(), oldPerfBonus, perfBonus);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 补齐绩效记录的冗余字段，确保只传 empId 也能正常新增/修改。
     */
    private void fillSnapshotFields(Performance perf, Long managerId) {
        if (perf.getEmpId() == null) {
            throw new RuntimeException("绩效记录缺少员工ID");
        }
        Employee emp = employeeMapper.selectById(perf.getEmpId());
        if (emp == null) {
            throw new RuntimeException("员工不存在：empId=" + perf.getEmpId());
        }
        perf.setEmpNo(emp.getEmpNo());
        perf.setEmpName(emp.getRealName());
        perf.setDeptId(emp.getDeptId());
        if (managerId != null) {
            perf.setManagerId(managerId);
            User manager = userMapper.selectById(managerId);
            if (manager != null) {
                perf.setManagerName(manager.getRealName());
            }
        }
        if (perf.getStatus() == null) {
            perf.setStatus(2);
        }
    }
}
