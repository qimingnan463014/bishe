package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.*;
import com.salary.mapper.*;
import com.salary.service.SalaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 薪资计算 ServiceImpl
 * <p>
 * 算薪公式（严格按照业务需求）：
 * <pre>
 *  社保     = 部门工资基数（dept.baseSalary） × 17.5%
 *  津贴     = 固定值（从薪资结构表读取）
 *  绩效     = 关联绩效表的 perfBonusRatio × 部门基数 × 绩效占比（默认20%）
 *  考勤扣款 = 迟到次数 × 50 + 早退次数 × 50 - 加班时长 × 20（加班为奖励，可为负扣款）
 *  个税     = 跨月累计预扣预缴法（七级超额累进税率）
 *  实发工资 = 基本工资（部门基数） + 津贴 + 绩效 - 社保 - 考勤扣款 - 个税
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryServiceImpl extends ServiceImpl<SalaryRecordMapper, SalaryRecord>
        implements SalaryService {

    // ====================================================
    // 薪资计算常量
    // ====================================================
    /** 社保缴纳比例：17.5%（个人缴纳，不可手动修改） */
    private static final BigDecimal SOCIAL_SECURITY_RATIO = new BigDecimal("0.175");

    /** 迟到/早退每次扣款（元） */
    private static final BigDecimal LATE_DEDUCT_PER_TIME = new BigDecimal("50");

    /** 加班每小时奖励（元） */
    private static final BigDecimal OVERTIME_PAY_PER_HOUR = new BigDecimal("20");

    /** 个人所得税起征点（元/月） */
    private static final BigDecimal TAX_THRESHOLD_PER_MONTH = new BigDecimal("5000");



    // 七级累进税率（年度应纳税所得额对应）
    // [上限, 税率, 速算扣除数]
    private static final BigDecimal[][] TAX_BRACKETS = {
            {new BigDecimal("36000"),  new BigDecimal("0.03"),  BigDecimal.ZERO},
            {new BigDecimal("144000"), new BigDecimal("0.10"),  new BigDecimal("2520")},
            {new BigDecimal("300000"), new BigDecimal("0.20"),  new BigDecimal("16920")},
            {new BigDecimal("420000"), new BigDecimal("0.25"),  new BigDecimal("31920")},
            {new BigDecimal("660000"), new BigDecimal("0.30"),  new BigDecimal("52920")},
            {new BigDecimal("960000"), new BigDecimal("0.35"),  new BigDecimal("85920")},
            {null,                     new BigDecimal("0.45"),  new BigDecimal("181920")}, // 960000以上
    };

    private final SalaryRecordMapper        salaryRecordMapper;
    private final EmployeeMapper            employeeMapper;
    private final DepartmentMapper          departmentMapper;
    private final AttendanceRecordMapper    attendanceRecordMapper;
    private final PerformanceMapper         performanceMapper;
    private final TaxAccumulateMapper       taxAccumulateMapper;
    private final SalaryPaymentMapper       salaryPaymentMapper;
    private final UserMapper                userMapper;

    // ====================================================
    //  查询
    // ====================================================

    @Override
    public PageResult<SalaryRecord> page(int current, int size,
                                          String yearMonth, String empNo, String realName,
                                          Long deptId, Integer calcStatus, Long managerId) {
        Page<SalaryRecord> page = new Page<>(current, size);
        return PageResult.of(salaryRecordMapper.selectPageWithCondition(
                page, yearMonth, empNo, realName, deptId, calcStatus, managerId));
    }

    @Override
    public SalaryRecord getDetail(Long id) { return getById(id); }

    @Override
    public PageResult<SalaryRecord> getMyHistory(int current, int size, Long empId) {
        Page<SalaryRecord> page = new Page<>(current, size);
        return PageResult.of(salaryRecordMapper.selectByEmpId(page, empId));
    }

    @Override
    public SalaryRecord getByEmpAndMonth(Long empId, String yearMonth) {
        return salaryRecordMapper.selectByEmpAndMonth(empId, yearMonth);
    }

    // ====================================================
    //  ★ 核心：自动薪资计算
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalaryRecord calculateSalary(Long empId, String yearMonth) {
        log.info("【算薪开始】empId={}, yearMonth={}", empId, yearMonth);

        // ── 1. 加载员工档案 ──────────────────────────────────────────
        Employee emp = employeeMapper.selectById(empId);
        if (emp == null) throw new RuntimeException("员工不存在：empId=" + empId);

        // ── 2. 加载部门（获取部门工资基数）────────────────────────────
        Department dept = departmentMapper.selectById(emp.getDeptId());
        if (dept == null) throw new RuntimeException("部门不存在：deptId=" + emp.getDeptId());

        // 部门工资基数 ≡ 员工本月基本工资（按需求规定）
        BigDecimal baseSalary = dept.getBaseSalary();

        // ── 3. 读取当月考勤汇总 ────────────────────────────────────────
        AttendanceRecord attendance = attendanceRecordMapper.selectByEmpAndMonth(empId, yearMonth);

        // ===== ★ 考勤扣款与加班奖励计算 =====
        //
        //  迟到次数 × 50元 = 扣款
        //  早退次数 × 50元 = 扣款（存储在 lateTimes 字段，本项目中迟到/早退均记在lateTimes）
        //  加班时长 × 20元 = 奖励（从 attend_deduct 中抵扣，即负扣款）
        //
        //  注：由于前端考勤表中只有 late_times 字段，早退记录与迟到合并计入 late_times
        //  若后续需要区分，可在 DTO 中扩展 earlyLeaveTimes 字段
        //
        BigDecimal attendDeduct = BigDecimal.ZERO;
        BigDecimal overtimePay  = BigDecimal.ZERO;

        if (attendance != null) {
            int lateTimes = attendance.getLateTimes() != null ? attendance.getLateTimes() : 0;
            // 迟到/早退扣款 = 次数 × 50
            BigDecimal lateDeduct = LATE_DEDUCT_PER_TIME.multiply(BigDecimal.valueOf(lateTimes));

            // 加班奖励 = 加班小时数 × 20（加班时已向下取整为整小时）
            BigDecimal overtimeHours = attendance.getOvertimeHours() != null
                    ? attendance.getOvertimeHours() : BigDecimal.ZERO;
            overtimePay = OVERTIME_PAY_PER_HOUR.multiply(overtimeHours);

            // 净考勤扣款（可以为负，即加班奖励 > 迟到扣款时）
            // 但实发工资公式中"扣款"项不含加班奖励，分开处理
            attendDeduct = lateDeduct;

            log.info("  考勤扣款：迟到/早退{}次 × 50 = {} 元; 加班{}小时 × 20 = {} 元",
                    lateTimes, lateDeduct, overtimeHours, overtimePay);
        } else {
            log.warn("  未找到考勤记录，empId={} yearMonth={}，考勤扣款/加班奖励均为0", empId, yearMonth);
        }

        // ── 4. 读取绩效评分 → 计算绩效奖金 ────────────────────────────
        //
        //  绩效奖金 = 管理员根据员工绩效等级设置的绝对金额 (读取 perfBonusRatio)
        //
        BigDecimal perfBonus = BigDecimal.ZERO;
        Performance perf = performanceMapper.selectByEmpAndMonth(empId, yearMonth);
        if (perf != null && perf.getPerfBonusRatio() != null) {
            perfBonus = perf.getPerfBonusRatio().setScale(2, RoundingMode.HALF_UP);
            log.info("  绩效奖金：根据绩效等级获取绝对金额 = {} 元", perfBonus);
        } else {
            log.info("  未找到绩效记录，绩效奖金为0（empId={} yearMonth={}）", empId, yearMonth);
        }

        // ── 5. 津贴（部门经理手动录入的津贴）────────────
        // 查询当月是否已有已存在的薪资记录（经理可能已录入津贴草稿）
        SalaryRecord existingRecord = salaryRecordMapper.selectByEmpAndMonth(empId, yearMonth);
        BigDecimal allowance = (existingRecord != null && existingRecord.getAllowance() != null)
                ? existingRecord.getAllowance() : BigDecimal.ZERO;

        // ── 6. ★ 社保计算（不可手动修改）──────────────────────────────
        //
        //  社保 = 部门工资基数 × 17.5%
        //  此处直接使用部门基数作为社保计算基数，与员工实际基本工资无关
        //
        BigDecimal socialSecurityEmp = baseSalary
                .multiply(SOCIAL_SECURITY_RATIO)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("  社保：部门基数{} × 17.5% = {} 元", baseSalary, socialSecurityEmp);

        // ── 7. 应发合计 ────────────────────────────────────────────────
        //  应发合计 = ①基本工资 + ③绩效奖金 + ④加班奖励 + ⑥部门经理录入的津贴
        BigDecimal grossSalary = baseSalary
                .add(allowance)
                .add(perfBonus)
                .add(overtimePay);
        log.info("  应发工资 = {} + {} + {} + {} = {}",
                baseSalary, allowance, perfBonus, overtimePay, grossSalary);

        // ── 8. ★ 跨月累计预扣个税（核心算法）──────────────────────────────────────
        BigDecimal incomeTax = calculateIncomeTax(emp, yearMonth, grossSalary, socialSecurityEmp);

        // ── 9. 实发工资 = ⑥应发合计 - ②社保 - ⑤考勤扣款 - ⑦本月实扣个税 ────────────
        BigDecimal totalDeduct = socialSecurityEmp.add(attendDeduct).add(incomeTax);
        BigDecimal netSalary   = grossSalary.subtract(socialSecurityEmp)
                .subtract(attendDeduct)
                .subtract(incomeTax)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("  实发 = {} - {} - {} - {} = {}",
                grossSalary, socialSecurityEmp, attendDeduct, incomeTax, netSalary);

        // ── 10. 构建并保存薪资记录 ─────────────────────────────────────
        SalaryRecord record;
        boolean isNew = false;
        if (existingRecord == null) {
            record = new SalaryRecord();
            isNew = true;
        } else {
            record = existingRecord;
        }

        record.setYearMonth(yearMonth);
        record.setEmpId(emp.getId());
        record.setEmpNo(emp.getEmpNo());
        record.setEmpName(emp.getRealName());
        record.setDeptId(emp.getDeptId());
        record.setDeptName(dept.getDeptName());
        record.setBankAccount(emp.getBankAccount());
        Long targetManagerId = emp.getManagerId() != null ? emp.getManagerId() : dept.getManagerId();
        record.setManagerId(targetManagerId);
        if (targetManagerId != null) {
            User manager = userMapper.selectById(targetManagerId);
            if (manager != null) {
                record.setManagerNo(manager.getUsername());
                record.setManagerName(manager.getRealName());
            }
        }
        record.setAttendanceId(attendance != null ? attendance.getId() : null);
        record.setBaseSalary(baseSalary);
        record.setOvertimePay(overtimePay);
        record.setPerfBonus(perfBonus);
        record.setFullAttendBonus(BigDecimal.ZERO); // 全勤奖逻辑如需可扩展
        record.setAllowance(allowance);
        record.setOtherIncome(BigDecimal.ZERO);
        record.setGrossSalary(grossSalary);
        record.setSocialSecurityEmp(socialSecurityEmp);
        record.setAttendDeduct(attendDeduct);
        record.setOtherDeduct(BigDecimal.ZERO);
        record.setIncomeTax(incomeTax);
        record.setTotalDeduct(totalDeduct);
        record.setNetSalary(netSalary);
        record.setCalcStatus(1); // 草稿，等待审核
        record.setRecordDate(LocalDate.now());

        if (isNew) { save(record); } else { updateById(record); }
        log.info("【算薪完成】empNo={} yearMonth={} 实发={}", emp.getEmpNo(), yearMonth, netSalary);
        return record;
    }

    // ====================================================
    //  ★ 跨月累计预扣个税（内部方法）
    // ====================================================

    /**
     * 按国家"累计预扣预缴法"计算本月实际应缴个税
     * <p>
     * 步骤：
     * <pre>
     * 1. 推算本年度该员工在职月份数 N（从入职月或1月取较晚值，到本月）
     * 2. 累计免税额 = N × 5000
     * 3. 累计总收入 = 上月累计应发工资 + 本月应发工资
     * 4. 累计总社保 = 上月累计社保 + 本月社保
     * 5. 累计应纳税所得额 = 累计总收入 - 累计总社保 - 累计免税额
     * 6. 本年应缴税额 = 套用七级税率表（超额累进）
     * 7. 本月税款 = 本年应缴税额 - 上月累计已缴税额（最小为0）
     * </pre>
     *
     * @param emp              员工档案（含 hireDate）
     * @param yearMonth        本月（YYYY-MM）
     * @param grossSalary      本月应发工资
     * @param socialSecurityEmp 本月个人社保
     * @return 本月应预扣个税金额（≥ 0）
     */
    private BigDecimal calculateIncomeTax(Employee emp,
                                           String yearMonth,
                                           BigDecimal grossSalary,
                                           BigDecimal socialSecurityEmp) {
        // ─── 推算本税务年度的在职月份数 N ─────────────────────────────
        YearMonth currentYM  = YearMonth.parse(yearMonth);
        int taxYear          = currentYM.getYear();
        // 当年1月（或入职月，取较晚者）
        YearMonth startOfTaxYear;
        if (emp.getHireDate() != null) {
            YearMonth hireYM = YearMonth.of(emp.getHireDate().getYear(), emp.getHireDate().getMonth());
            YearMonth janOfYear = YearMonth.of(taxYear, 1);
            startOfTaxYear = hireYM.isAfter(janOfYear) ? hireYM : janOfYear;
        } else {
            startOfTaxYear = YearMonth.of(taxYear, 1);
        }
        // 在职月份数 N（含本月）
        long monthsInService = startOfTaxYear.until(currentYM,
                java.time.temporal.ChronoUnit.MONTHS) + 1;
        monthsInService = Math.max(monthsInService, 1L);

        // ─── 读取上月累计个税数据 ─────────────────────────────────────
        String prevMonth = currentYM.minusMonths(1).toString(); // YYYY-MM
        TaxAccumulate prev = taxAccumulateMapper.selectPrevMonthAccumulate(
                emp.getId(), String.valueOf(taxYear), prevMonth);

        BigDecimal accumGross  = (prev != null && prev.getAccumGross() != null)
                ? prev.getAccumGross() : BigDecimal.ZERO;
        BigDecimal accumSocial = (prev != null && prev.getAccumSocialSecurity() != null)
                ? prev.getAccumSocialSecurity() : BigDecimal.ZERO;
        BigDecimal accumTax    = (prev != null && prev.getAccumTax() != null)
                ? prev.getAccumTax() : BigDecimal.ZERO;

        // ─── 本年累计数据（加上本月） ─────────────────────────────────
        BigDecimal newAccumGross  = accumGross.add(grossSalary);
        BigDecimal newAccumSocial = accumSocial.add(socialSecurityEmp);

        // 累计免税额 = 在职月份数 × 5000
        BigDecimal accumExempt = TAX_THRESHOLD_PER_MONTH.multiply(BigDecimal.valueOf(monthsInService));

        // 累计应纳税所得额
        BigDecimal accumTaxable = newAccumGross.subtract(newAccumSocial).subtract(accumExempt);
        if (accumTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("  个税：累计应纳税所得额 ≤ 0，本月免税");
            saveTaxAccumulate(emp.getId(), yearMonth, String.valueOf(taxYear),
                    BigDecimal.ZERO, BigDecimal.ZERO, grossSalary, socialSecurityEmp,
                    newAccumGross, newAccumSocial, accumTax);
            return BigDecimal.ZERO;
        }

        // ─── 套用七级累进税率（算本年应缴总税额）──────────────────────
        BigDecimal yearTotalTax = calcProgressiveTax(accumTaxable);

        // ─── 本月实扣 = 本年应缴总税 - 上月累计已缴税（最小值0）────────
        BigDecimal monthTax = yearTotalTax.subtract(accumTax).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 本年新的累计已缴税额
        BigDecimal newAccumTax = accumTax.add(monthTax);

        log.info("  个税：在职{}月 累计应税所得={} 年应税={} 累计已缴={} 本月税款={}",
                monthsInService, accumTaxable, yearTotalTax, accumTax, monthTax);

        // ─── 保存累计记录 ─────────────────────────────────────────────
        saveTaxAccumulate(emp.getId(), yearMonth, String.valueOf(taxYear),
                grossSalary.subtract(socialSecurityEmp).subtract(TAX_THRESHOLD_PER_MONTH), // 本月应纳税所得
                monthTax, grossSalary, socialSecurityEmp,
                newAccumGross, newAccumSocial, newAccumTax);

        return monthTax;
    }

    /**
     * 按七级超额累进税率（年度应纳税所得额）计算应缴税额
     * <p>
     * 税率表（2024年综合所得）：
     * <pre>
     *  [0,       36000]  → 3%,  速算0
     *  (36000,  144000]  → 10%, 速算2520
     *  (144000, 300000]  → 20%, 速算16920
     *  (300000, 420000]  → 25%, 速算31920
     *  (420000, 660000]  → 30%, 速算52920
     *  (660000, 960000]  → 35%, 速算85920
     *  (960000, ∞)       → 45%, 速算181920
     * </pre>
     *
     * @param accTaxable 年度累计应纳税所得额（已减去免税额和专项扣除）
     * @return 年度应缴税额
     */
    private BigDecimal calcProgressiveTax(BigDecimal accTaxable) {
        for (BigDecimal[] bracket : TAX_BRACKETS) {
            BigDecimal ceiling  = bracket[0]; // null = 无上限（最高级）
            BigDecimal rate     = bracket[1];
            BigDecimal quickDed = bracket[2];
            if (ceiling == null || accTaxable.compareTo(ceiling) <= 0) {
                // 应缴税额 = 累计应纳税所得额 × 税率 - 速算扣除数
                return accTaxable.multiply(rate).subtract(quickDed)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        // 保底（理论上不会走到这里）
        return BigDecimal.ZERO;
    }

    /**
     * 保存/更新个税累计记录（t_tax_accumulate）
     */
    private void saveTaxAccumulate(Long empId, String yearMonth, String taxYear,
                                    BigDecimal monthTaxable, BigDecimal monthTax,
                                    BigDecimal monthGross, BigDecimal monthSocial,
                                    BigDecimal newAccumGross, BigDecimal newAccumSocial,
                                    BigDecimal newAccumTax) {
        TaxAccumulate ta = taxAccumulateMapper.selectOne(
                new LambdaQueryWrapper<TaxAccumulate>()
                        .eq(TaxAccumulate::getEmpId, empId)
                        .eq(TaxAccumulate::getYearMonth, yearMonth));

        if (ta == null) ta = new TaxAccumulate();
        ta.setEmpId(empId);
        ta.setTaxYear(taxYear);
        ta.setYearMonth(yearMonth);
        ta.setMonthTaxableIncome(monthTaxable);
        ta.setMonthTax(monthTax);
        ta.setMonthSocialSecurity(monthSocial);
        ta.setMonthFund(BigDecimal.ZERO); // 公积金如需单独列可扩展
        ta.setMonthSpecialDeduct(BigDecimal.ZERO);
        ta.setAccumGross(newAccumGross);
        ta.setAccumSocialSecurity(newAccumSocial);
        ta.setAccumTax(newAccumTax);
        ta.setAccumTaxableIncome(newAccumGross.subtract(newAccumSocial));
        ta.setAccumSpecialDeduct(BigDecimal.ZERO);

        if (ta.getId() == null) {
            taxAccumulateMapper.insert(ta);
        } else {
            taxAccumulateMapper.updateById(ta);
        }
    }

    // ====================================================
    //  批量算薪 / 发放 / 手动修改
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCalculate(String yearMonth, Long deptId) {
        List<Employee> employees = (deptId != null)
                ? employeeMapper.selectByDeptId(deptId)
                : employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getStatus, 1));
        log.info("批量算薪：yearMonth={} deptId={} 共{}人", yearMonth, deptId, employees.size());
        for (Employee emp : employees) {
            try {
                calculateSalary(emp.getId(), yearMonth);
            } catch (Exception e) {
                log.error("员工{}算薪失败：{}", emp.getEmpNo(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueSalary(Long salaryRecordId, Long operatorId) {
        SalaryRecord record = getById(salaryRecordId);
        if (record == null) throw new RuntimeException("薪资记录不存在");
        if (record.getCalcStatus() == 4)
            throw new RuntimeException("该账单已发放，无需重复支付");
        if (record.getCalcStatus() != 3)
            throw new RuntimeException("薪资单未经二审通过(当前状态=" + record.getCalcStatus() + ")，严禁发放！");

        record.setCalcStatus(4);
        record.setPayDate(LocalDate.now());
        updateById(record);

        Employee emp = employeeMapper.selectById(record.getEmpId());
        SalaryPayment payment = new SalaryPayment();
        payment.setSalaryRecordId(salaryRecordId);
        payment.setEmpId(record.getEmpId());
        payment.setEmpNo(record.getEmpNo());
        payment.setEmpName(record.getEmpName());
        payment.setYearMonth(record.getYearMonth());
        payment.setNetSalary(record.getNetSalary());
        payment.setBankAccount(record.getBankAccount());
        payment.setBankName(emp != null ? emp.getBankName() : null);
        payment.setPayDate(LocalDate.now());
        payment.setPayMethod(1);
        payment.setPayStatus(2);
        payment.setOperatorId(operatorId);
        salaryPaymentMapper.insert(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueBatch(List<Long> ids, Long operatorId) {
        ids.forEach(id -> {
            try { issueSalary(id, operatorId); }
            catch (Exception e) { log.error("发放失败 id={}：{}", id, e.getMessage()); }
        });
    }

    // ====================================================
    //  ★ 薪资状态流转：一键发布（经理 → 员工可见）
    // ====================================================

    /**
     * 一键发布薪资：草稿(1) → 已发布(2)
     * <p>
     * 调用者须为部门经理（role=2）或管理员（role=1）。
     * 发布后，普通员工的接口才可查询到自己的工资条（接口按 calcStatus >= 2 过滤）。
     *
     * @param salaryId 薪资记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishSalary(Long salaryId) {
        SalaryRecord record = getById(salaryId);
        if (record == null) throw new RuntimeException("薪资记录不存在：id=" + salaryId);
        if (record.getCalcStatus() != 1) {
            throw new RuntimeException("当前状态不允许发布（需为草稿/未发布），当前状态=" + record.getCalcStatus());
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setCalcStatus(2); // 2 = 已发布（员工可见）
        updateById(update);
        log.info("薪资已发布：id={} empNo={} yearMonth={}", salaryId, record.getEmpNo(), record.getYearMonth());
    }

    /**
     * 批量一键发布
     *
     * @param ids 薪资记录ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishBatch(List<Long> ids) {
        ids.forEach(id -> {
            try { publishSalary(id); }
            catch (Exception e) { log.error("发布失败 id={}：{}", id, e.getMessage()); }
        });
    }

    // ====================================================
    //  ★ 薪资状态流转：二审（仅管理员 Admin 可操作）
    // ====================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSalary(Long salaryId, Integer role) {
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能二审通过薪资");
        }
        SalaryRecord record = getById(salaryId);
        if (record == null) throw new RuntimeException("薪资记录不存在：id=" + salaryId);
        if (record.getCalcStatus() != 2) {
            throw new RuntimeException("当前状态不允许审核（需为已发布待二审），当前状态=" + record.getCalcStatus());
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setCalcStatus(3); // 3 = 已审核
        updateById(update);
        log.info("薪资已二审通过：id={} empNo={} yearMonth={}", salaryId, record.getEmpNo(), record.getYearMonth());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBatch(List<Long> ids, Integer role) {
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能执行批量二审");
        }
        ids.forEach(id -> {
            try { auditSalary(id, role); }
            catch (Exception e) { log.error("二审失败 id={}：{}", id, e.getMessage()); }
        });
    }

    // ====================================================
    //  ★ 薪资状态流转：支付（仅管理员 Admin 可操作）
    // ====================================================

    /**
     * 执行薪资支付：已审核(3) → 已发放(4)
     * <p>
     * ⚠ 角色严格校验：仅允许 role=1（管理员）操作，部门经理（role=2）无权调用。
     * Controller 层需在调用此方法前，从 SecurityContext 提取当前用户角色并传入。
     *
     * @param salaryId   薪资记录ID
     * @param operatorId 操作人用户ID（从JWT解析）
     * @param role       操作人角色：1=管理员，2=经理（经理调用抛异常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySalary(Long salaryId, Long operatorId, Integer role) {
        // ── 权限校验：仅管理员（role=1）才能支付，经理（role=2）无权 ──
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能执行薪资支付操作");
        }
        issueSalary(salaryId, operatorId);
        log.info("薪资支付完成（Admin操作）：id={} operatorId={}", salaryId, operatorId);
    }

    /**
     * 批量支付（仅管理员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBatch(List<Long> ids, Long operatorId, Integer role) {
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能执行批量支付");
        }
        ids.forEach(id -> {
            try { paySalary(id, operatorId, role); }
            catch (Exception e) { log.error("批量支付失败 id={}：{}", id, e.getMessage()); }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualUpdate(SalaryRecord record) {
        BigDecimal gross = record.getBaseSalary()
                .add(nvl(record.getOvertimePay()))
                .add(nvl(record.getPerfBonus()))
                .add(nvl(record.getFullAttendBonus()))
                .add(nvl(record.getAllowance()))
                .add(nvl(record.getOtherIncome()));
        BigDecimal deduct = nvl(record.getSocialSecurityEmp())
                .add(nvl(record.getAttendDeduct()))
                .add(nvl(record.getOtherDeduct()))
                .add(nvl(record.getIncomeTax()));
        record.setGrossSalary(gross);
        record.setTotalDeduct(deduct);
        record.setNetSalary(gross.subtract(deduct).setScale(2, RoundingMode.HALF_UP));
        updateById(record);
    }

    @Override
    public List<java.util.Map<String, Object>> getMonthlyTrend() {
        return salaryRecordMapper.countMonthlyTrend();
    }

    @Override
    public java.util.Map<String, Object> getSalaryStructure(String yearMonth) {
        return salaryRecordMapper.sumCurrentMonthStructure(yearMonth);
    }

    @Override
    public List<java.util.Map<String, Object>> getDeptAvgSalary() {
        return salaryRecordMapper.avgSalaryByDepartment();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
