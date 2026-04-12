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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 薪资计算 ServiceImpl
 * <p>
 * 算薪公式（严格按照当前业务口径）：
 * <pre>
 *  基本工资 = 部门基础工资；若员工为该部门经理，则基础工资 + 岗位工资
 *  津贴     = 薪资草稿中的 allowance（统一口径，不再区分“其他补助”）
 *  绩效     = 绩效评分模块已落库的绝对奖金金额 perfBonusRatio
 *  加班工资 = 考勤数据中的 overtimeHours × 20
 *  考勤扣款 = 优先读取考勤记录的 attendDeduct；若未维护则回退为 lateTimes × 50
 *  个税     = 跨月累计预扣预缴法（七级超额累进税率）
 *  实发工资 = 基本工资 + 津贴 + 绩效 + 加班工资 - 社保 - 考勤扣款 - 其他扣款 - 个税
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
    /** 个人养老保险：8% */
    private static final BigDecimal PENSION_RATIO = new BigDecimal("0.08");
    /** 个人医疗保险：2% */
    private static final BigDecimal MEDICAL_RATIO = new BigDecimal("0.02");
    /** 个人失业保险：0.3% */
    private static final BigDecimal UNEMPLOYMENT_RATIO = new BigDecimal("0.003");
    /** 个人住房公积金：12% */
    private static final BigDecimal HOUSING_FUND_RATIO = new BigDecimal("0.12");
    /** 个人五险一金税前扣除合计：22.3%（不含工伤、生育，个人不缴） */
    private static final BigDecimal PERSONAL_SOCIAL_TOTAL_RATIO = PENSION_RATIO
            .add(MEDICAL_RATIO)
            .add(UNEMPLOYMENT_RATIO)
            .add(HOUSING_FUND_RATIO);

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
    private final AnnouncementMapper        announcementMapper;

    // ====================================================
    //  查询
    // ====================================================

    @Override
    public PageResult<SalaryRecord> page(int current, int size,
                                          String yearMonth, String empNo, String realName,
                                          Long deptId, Integer calcStatus, Long managerId,
                                          String excludeEmpNo, Boolean excludeDraft) {
        Page<SalaryRecord> page = new Page<>(current, size);
        return PageResult.of(salaryRecordMapper.selectPageWithCondition(
                page, yearMonth, empNo, realName, deptId, calcStatus, managerId, excludeEmpNo, excludeDraft));
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
        SalaryRecord record = salaryRecordMapper.selectByEmpAndMonth(empId, yearMonth);
        if (record == null) {
            return null;
        }
        return isSlipVisibleToEmployee(record) ? record : null;
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

        SalaryRecord existingRecord = salaryRecordMapper.selectByEmpAndMonth(empId, yearMonth);

        // 基本工资以部门信息为准；部门经理在部门基础工资上叠加岗位工资
        BigDecimal baseSalary = resolveBaseSalary(emp, dept);
        syncEmployeeBaseSalaryIfNeeded(emp, baseSalary);

        // ── 3. 读取当月考勤汇总 ────────────────────────────────────────
        AttendanceRecord attendance = attendanceRecordMapper.selectByEmpAndMonth(empId, yearMonth);

        // ===== ★ 考勤扣款与加班奖励计算 =====
        //
        //  迟到次数 × 50元 = 扣款
        //  早退次数 × 50元 = 扣款
        //  加班时长 × 20元 = 奖励（从 attend_deduct 中抵扣，即负扣款）
        BigDecimal attendDeduct = BigDecimal.ZERO;
        BigDecimal overtimePay  = BigDecimal.ZERO;

        if (attendance != null) {
            int lateTimes = attendance.getLateTimes() != null ? attendance.getLateTimes() : 0;
            int earlyLeaveTimes = attendance.getEarlyLeaveTimes() != null ? attendance.getEarlyLeaveTimes() : 0;
            // 迟到/早退扣款 = (迟到 + 早退) × 50
            BigDecimal lateDeduct = LATE_DEDUCT_PER_TIME.multiply(BigDecimal.valueOf(lateTimes + earlyLeaveTimes));
            BigDecimal recordedAttendDeduct = nvl(attendance.getAttendDeduct());

            // 加班奖励 = 加班小时数 × 20（加班时已向下取整为整小时）
            BigDecimal overtimeHours = attendance.getOvertimeHours() != null
                    ? attendance.getOvertimeHours() : BigDecimal.ZERO;
            overtimePay = OVERTIME_PAY_PER_HOUR.multiply(overtimeHours).setScale(2, RoundingMode.HALF_UP);

            // 考勤扣款优先使用考勤模块已汇总的扣款值；若未汇总则回退到迟到/早退次数扣款
            attendDeduct = recordedAttendDeduct.compareTo(BigDecimal.ZERO) > 0
                    ? recordedAttendDeduct.setScale(2, RoundingMode.HALF_UP)
                    : lateDeduct.setScale(2, RoundingMode.HALF_UP);

            log.info("  考勤扣款：迟到{}次 + 早退{}次基础扣款={} 元，落库扣款={} 元；加班{}小时 × 20 = {} 元",
                    lateTimes, earlyLeaveTimes, lateDeduct, attendDeduct, overtimeHours, overtimePay);
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
        // 查询当月是否已有已存在的薪资记录（经理可能已录入津贴/处罚草稿）
        BigDecimal allowance = (existingRecord != null && existingRecord.getAllowance() != null)
                ? existingRecord.getAllowance().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal otherDeduct = (existingRecord != null && existingRecord.getOtherDeduct() != null)
                ? existingRecord.getOtherDeduct().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // ── 6. ★ 五险一金个人扣款计算（不可手动修改）────────────────────
        //
        //  个人税前扣除 = 薪资基数 × (养老8% + 医疗2% + 失业0.3% + 公积金12%)
        //             = 薪资基数 × 22.3%
        //
        BigDecimal socialSecurityEmp = baseSalary
                .multiply(PERSONAL_SOCIAL_TOTAL_RATIO)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("  五险一金：薪资基数{} × 22.3% = {} 元", baseSalary, socialSecurityEmp);

        // ── 7. 应发合计 ────────────────────────────────────────────────
        //  应发合计 = ①基本工资 + ③绩效奖金 + ④加班奖励 + ⑥部门经理录入的津贴
        BigDecimal grossSalary = baseSalary
                .add(allowance)
                .add(perfBonus)
                .add(overtimePay)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("  应发工资 = {} + {} + {} + {} = {}",
                baseSalary, allowance, perfBonus, overtimePay, grossSalary);

        // ── 8. ★ 跨月累计预扣个税（核心算法）──────────────────────────────────────
        BigDecimal incomeTax = calculateIncomeTax(emp, yearMonth, baseSalary, grossSalary, socialSecurityEmp);

        // ── 9. 实发工资 = ⑥应发合计 - ②社保 - ⑤考勤扣款 - ⑦本月实扣个税 ────────────
        BigDecimal totalDeduct = socialSecurityEmp
                .add(attendDeduct)
                .add(otherDeduct)
                .add(incomeTax)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal netSalary   = grossSalary
                .subtract(totalDeduct)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("  实发 = {} - {} - {} - {} - {} = {}",
                grossSalary, socialSecurityEmp, attendDeduct, otherDeduct, incomeTax, netSalary);

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
        record.setOtherDeduct(otherDeduct);
        record.setIncomeTax(incomeTax);
        record.setTotalDeduct(totalDeduct);
        record.setNetSalary(netSalary);
        if (isNew) {
            record.setCalcStatus(1); // 仅新生成的工资单默认进入草稿
            record.setSlipPublished(0);
            record.setSlipPublishTime(null);
            record.setRecordDate(LocalDate.now());
        } else {
            if (record.getCalcStatus() == null) {
                record.setCalcStatus(1);
            }
            if (record.getSlipPublished() == null) {
                record.setSlipPublished(0);
            }
            if (record.getRecordDate() == null) {
                record.setRecordDate(LocalDate.now());
            }
        }

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
     * 5. 累计应纳税所得额 = 累计总收入 - 累计个人五险一金 - 累计免税额
     * 6. 本年应缴税额 = 套用七级税率表（超额累进）
     * 7. 本月税款 = 本年应缴税额 - 上月累计已缴税额（最小为0）
     * </pre>
     *
     * @param emp              员工档案（含 hireDate）
     * @param yearMonth        本月（YYYY-MM）
     * @param grossSalary      本月应发工资
     * 说明：专项附加扣除在本系统中暂不参与计算，但仍保留国家要求的累计预扣法。
     *
     * @param socialSecurityEmp 本月个人五险一金税前扣除
     * @return 本月应预扣个税金额（≥ 0）
     */
    private BigDecimal calculateIncomeTax(Employee emp,
                                           String yearMonth,
                                           BigDecimal baseSalary,
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

        // 累计免税额 = 在职月份数 × 5000（入职前月份收入与起征额均按0处理）
        BigDecimal accumExempt = TAX_THRESHOLD_PER_MONTH.multiply(BigDecimal.valueOf(monthsInService));

        // 累计应纳税所得额
        BigDecimal accumTaxable = newAccumGross.subtract(newAccumSocial).subtract(accumExempt);
        if (accumTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("  个税：累计应纳税所得额 ≤ 0，本月免税");
            saveTaxAccumulate(emp.getId(), yearMonth, String.valueOf(taxYear),
                    baseSalary, BigDecimal.ZERO, BigDecimal.ZERO, grossSalary, socialSecurityEmp,
                    newAccumGross, newAccumSocial, BigDecimal.ZERO, accumTax);
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
        BigDecimal monthTaxableIncome = grossSalary
                .subtract(socialSecurityEmp)
                .subtract(TAX_THRESHOLD_PER_MONTH)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        saveTaxAccumulate(emp.getId(), yearMonth, String.valueOf(taxYear),
                baseSalary, monthTaxableIncome, monthTax, grossSalary, socialSecurityEmp,
                newAccumGross, newAccumSocial, accumTaxable, newAccumTax);

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
     * @param accTaxable 年度累计应纳税所得额（已减去个人五险一金与5000/月起征额，不含专项附加扣除）
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
                                    BigDecimal baseSalary, BigDecimal monthTaxable, BigDecimal monthTax,
                                    BigDecimal monthGross, BigDecimal monthSocial,
                                    BigDecimal newAccumGross, BigDecimal newAccumSocial,
                                    BigDecimal accumTaxableIncome, BigDecimal newAccumTax) {
        BigDecimal safeBaseSalary = nvl(baseSalary).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthFund = safeBaseSalary.multiply(HOUSING_FUND_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthInsurance = nvl(monthSocial).subtract(monthFund).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
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
        ta.setMonthSocialSecurity(monthInsurance);
        ta.setMonthFund(monthFund);
        ta.setMonthSpecialDeduct(BigDecimal.ZERO);
        ta.setAccumGross(newAccumGross);
        ta.setAccumSocialSecurity(newAccumSocial);
        ta.setAccumTax(newAccumTax);
        ta.setAccumTaxableIncome(accumTaxableIncome);
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
        record.setSlipPublished(0);
        record.setSlipPublishTime(null);
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
        if (record.getCalcStatus() != 1 && record.getCalcStatus() != 5) {
            throw new RuntimeException("当前状态不允许提交审核（需为草稿或已驳回），当前状态=" + record.getCalcStatus());
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setCalcStatus(2); // 2 = 待审核
        updateById(update);
        log.info("薪资已提交审核：id={} empNo={} yearMonth={}", salaryId, record.getEmpNo(), record.getYearMonth());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectSalary(Long salaryId, Integer role, String reason) {
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能驳回薪资");
        }
        SalaryRecord record = getById(salaryId);
        if (record == null) {
            throw new RuntimeException("薪资记录不存在：id=" + salaryId);
        }
        if (record.getCalcStatus() != 2) {
            throw new RuntimeException("当前状态不允许驳回（需为待审核），当前状态=" + record.getCalcStatus());
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setCalcStatus(5); // 5 = 已驳回
        update.setRemark(buildRejectRemark(record.getRemark(), reason));
        updateById(update);
        log.info("薪资已驳回：id={} empNo={} yearMonth={} reason={}", salaryId, record.getEmpNo(), record.getYearMonth(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBatch(List<Long> ids, Integer role, String reason) {
        if (role == null || role != 1) {
            throw new RuntimeException("权限不足：只有管理员（Admin）才能执行批量驳回");
        }
        ids.forEach(id -> {
            try { rejectSalary(id, role, reason); }
            catch (Exception e) { log.error("批量驳回失败 id={}：{}", id, e.getMessage()); }
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
    public void publishSalarySlip(Long salaryId, Long operatorId, String operatorName) {
        SalaryRecord record = getById(salaryId);
        if (record == null) {
            throw new RuntimeException("薪资记录不存在：id=" + salaryId);
        }
        if (record.getCalcStatus() != 4) {
            throw new RuntimeException("只有已发放的工资记录才允许发布工资条");
        }
        if (Integer.valueOf(1).equals(record.getSlipPublished())) {
            return;
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setSlipPublished(1);
        update.setSlipPublishTime(LocalDateTime.now());
        updateById(update);
        createSalarySlipAnnouncement(record, operatorId, operatorName);
        log.info("工资条已发布：salaryId={} empNo={} yearMonth={}", salaryId, record.getEmpNo(), record.getYearMonth());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishSalarySlips(List<Long> ids, Long operatorId, String operatorName) {
        for (Long id : ids) {
            try {
                publishSalarySlip(id, operatorId, operatorName);
            } catch (Exception e) {
                log.error("批量发布工资条失败 id={}：{}", id, e.getMessage());
            }
        }
    }

    private String buildRejectRemark(String currentRemark, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        String rejectRemark = normalizedReason.isEmpty() ? "审核驳回：请经理修改后重新提交。" : "审核驳回：" + normalizedReason;
        if (currentRemark == null || currentRemark.trim().isEmpty()) {
            return rejectRemark;
        }
        if (currentRemark.startsWith("审核驳回：")) {
            return rejectRemark;
        }
        return rejectRemark + "；原备注：" + currentRemark.trim();
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
    @Transactional(rollbackFor = Exception.class)
    public void updateIssueFile(Long salaryId, String issueFile, Integer role) {
        if (role == null || role != 1) {
            throw new RuntimeException("只有管理员才能上传发放文件");
        }
        SalaryRecord record = getById(salaryId);
        if (record == null) {
            throw new RuntimeException("薪资记录不存在：id=" + salaryId);
        }
        SalaryRecord update = new SalaryRecord();
        update.setId(salaryId);
        update.setIssueFile(issueFile);
        updateById(update);
    }

    @Override
    public List<java.util.Map<String, Object>> getMonthlyTrend(String yearMonth) {
        String targetYearMonth = yearMonth;
        if (targetYearMonth == null || targetYearMonth.trim().isEmpty()) {
            targetYearMonth = YearMonth.now().toString();
        }
        return salaryRecordMapper.countMonthlyTrend(targetYearMonth);
    }

    @Override
    public java.util.Map<String, Object> getSalaryStructure(String yearMonth) {
        return salaryRecordMapper.sumCurrentMonthStructure(yearMonth);
    }

    @Override
    public List<java.util.Map<String, Object>> getDeptAvgSalary(String yearMonth) {
        String targetYearMonth = yearMonth;
        if (targetYearMonth == null || targetYearMonth.trim().isEmpty()) {
            targetYearMonth = YearMonth.now().toString();
        }
        return salaryRecordMapper.avgSalaryByDepartment(targetYearMonth);
    }

    private boolean isSlipVisibleToEmployee(SalaryRecord record) {
        return record != null
                && Integer.valueOf(4).equals(record.getCalcStatus())
                && Integer.valueOf(1).equals(record.getSlipPublished());
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void createSalarySlipAnnouncement(SalaryRecord record, Long operatorId, String operatorName) {
        String title = String.format("%s 工资条已发布", record.getYearMonth());
        Announcement existing = announcementMapper.selectOne(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getTitle, title)
                        .eq(Announcement::getStatus, 1)
                        .orderByDesc(Announcement::getPubTime)
                        .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(String.format(
                "各位同事：%s 的工资条现已开放查询，请登录系统进入“我的薪水”查看对应月份的工资明细。",
                record.getYearMonth()));
        announcement.setCoverImage(null);
        announcement.setPubUserId(operatorId);
        announcement.setPubUserName((operatorName == null || operatorName.trim().isEmpty()) ? "系统管理员" : operatorName);
        announcement.setTargetRole(0);
        announcement.setIsTop(0);
        announcement.setStatus(1);
        announcement.setPubTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
    }

    private BigDecimal resolveBaseSalary(Employee emp, Department dept) {
        BigDecimal baseSalary = nvl(dept.getBaseSalary());
        if (isDepartmentManager(emp, dept)) {
            baseSalary = baseSalary.add(nvl(dept.getPositionSalary()));
        }
        return baseSalary.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isDepartmentManager(Employee emp, Department dept) {
        return emp != null
                && emp.getUserId() != null
                && dept != null
                && dept.getManagerId() != null
                && dept.getManagerId().equals(emp.getUserId());
    }

    private void syncEmployeeBaseSalaryIfNeeded(Employee emp, BigDecimal expectedBaseSalary) {
        if (emp == null || emp.getId() == null) {
            return;
        }
        BigDecimal currentBaseSalary = nvl(emp.getBaseSalary()).setScale(2, RoundingMode.HALF_UP);
        if (currentBaseSalary.compareTo(expectedBaseSalary) == 0) {
            return;
        }
        Employee update = new Employee();
        update.setId(emp.getId());
        update.setBaseSalary(expectedBaseSalary);
        employeeMapper.updateById(update);
        emp.setBaseSalary(expectedBaseSalary);
        log.info("  同步员工档案基础工资：empId={} {} -> {}", emp.getId(), currentBaseSalary, expectedBaseSalary);
    }
}
