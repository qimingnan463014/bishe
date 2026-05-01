package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.SocialSecurityConfig;
import com.salary.mapper.SocialSecurityConfigMapper;
import com.salary.service.SocialSecurityConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class SocialSecurityConfigServiceImpl extends ServiceImpl<SocialSecurityConfigMapper, SocialSecurityConfig> implements SocialSecurityConfigService {

    private static final BigDecimal DEFAULT_PENSION_RATE = new BigDecimal("0.08");
    private static final BigDecimal DEFAULT_MEDICAL_RATE = new BigDecimal("0.02");
    private static final BigDecimal DEFAULT_UNEMPLOYMENT_RATE = new BigDecimal("0.003");
    private static final BigDecimal DEFAULT_FUND_RATE = new BigDecimal("0.12");
    private static final BigDecimal DEFAULT_BASE_MIN = new BigDecimal("3000.00");
    private static final BigDecimal DEFAULT_BASE_MAX = new BigDecimal("30000.00");

    @Override
    public PageResult<SocialSecurityConfig> page(int current, int size, String configName) {
        Page<SocialSecurityConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<SocialSecurityConfig> qw = new LambdaQueryWrapper<>();
        if(StringUtils.hasText(configName)) {
            qw.like(SocialSecurityConfig::getConfigName, configName);
        }
        qw.orderByDesc(SocialSecurityConfig::getIsActive).orderByDesc(SocialSecurityConfig::getCreateTime);
        this.page(page, qw);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addConfig(SocialSecurityConfig config) {
        if(config.getIsActive() != null && config.getIsActive() == 1) {
            // 如果新加的是启用的状态，需要把其他的全置为不启用（这里假设全局只有一套正在生效的）
            LambdaQueryWrapper<SocialSecurityConfig> updateQw = new LambdaQueryWrapper<>();
            updateQw.eq(SocialSecurityConfig::getIsActive, 1);
            SocialSecurityConfig updateEntity = new SocialSecurityConfig();
            updateEntity.setIsActive(0);
            update(updateEntity, updateQw);
        }
        if(config.getEffectiveDate() == null) config.setEffectiveDate(LocalDate.now());
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SocialSecurityConfig config) {
        if(config.getIsActive() != null && config.getIsActive() == 1) {
            LambdaQueryWrapper<SocialSecurityConfig> updateQw = new LambdaQueryWrapper<>();
            updateQw.eq(SocialSecurityConfig::getIsActive, 1).ne(SocialSecurityConfig::getId, config.getId());
            SocialSecurityConfig updateEntity = new SocialSecurityConfig();
            updateEntity.setIsActive(0);
            update(updateEntity, updateQw);
        }
        updateById(config);
    }

    @Override
    public SocialSecurityConfig getCurrentPersonalConfig() {
        return resolvePersonalConfigForMonth(YearMonth.now());
    }

    @Override
    public SocialSecurityConfig resolvePersonalConfigForMonth(YearMonth yearMonth) {
        YearMonth target = yearMonth != null ? yearMonth : YearMonth.now();
        LocalDate effectiveDate = target.atDay(1);
        LambdaQueryWrapper<SocialSecurityConfig> query = new LambdaQueryWrapper<>();
        query.le(SocialSecurityConfig::getEffectiveDate, effectiveDate)
                .orderByDesc(SocialSecurityConfig::getEffectiveDate)
                .orderByDesc(SocialSecurityConfig::getIsActive)
                .orderByDesc(SocialSecurityConfig::getCreateTime)
                .last("LIMIT 1");
        SocialSecurityConfig config = getOne(query, false);
        if (config == null || isLegacyCompanyStyle(config)) {
            return buildDefaultPersonalConfig(effectiveDate);
        }
        normalizePersonalRates(config);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialSecurityConfig applyPersonalConfig(String effectiveMonth,
                                                    BigDecimal pensionRate,
                                                    BigDecimal medicalRate,
                                                    BigDecimal unemploymentRate,
                                                    BigDecimal fundRate) {
        YearMonth yearMonth = YearMonth.parse(effectiveMonth);
        if (yearMonth.isBefore(YearMonth.now())) {
            throw new RuntimeException("生效月份不能早于当前月份");
        }
        SocialSecurityConfig current = getCurrentPersonalConfig();

        LambdaQueryWrapper<SocialSecurityConfig> disableActiveQw = new LambdaQueryWrapper<>();
        disableActiveQw.eq(SocialSecurityConfig::getIsActive, 1);
        SocialSecurityConfig disableEntity = new SocialSecurityConfig();
        disableEntity.setIsActive(0);
        update(disableEntity, disableActiveQw);

        SocialSecurityConfig config = new SocialSecurityConfig();
        config.setConfigName("员工社保比例方案-" + effectiveMonth);
        config.setPensionEmpRatio(toRatioDecimal(pensionRate));
        config.setMedicalEmpRatio(toRatioDecimal(medicalRate));
        config.setUnemploymentEmpRatio(toRatioDecimal(unemploymentRate));
        config.setFundEmpRatio(toRatioDecimal(fundRate));
        config.setCalcBaseMin(current.getCalcBaseMin() != null ? current.getCalcBaseMin() : DEFAULT_BASE_MIN);
        config.setCalcBaseMax(current.getCalcBaseMax() != null ? current.getCalcBaseMax() : DEFAULT_BASE_MAX);
        config.setEffectiveDate(yearMonth.atDay(1));
        config.setIsActive(1);
        save(config);
        normalizePersonalRates(config);
        return config;
    }

    private void normalizePersonalRates(SocialSecurityConfig config) {
        if (config == null) {
            return;
        }
        if (config.getPensionEmpRatio() == null) {
            config.setPensionEmpRatio(DEFAULT_PENSION_RATE);
        }
        if (config.getMedicalEmpRatio() == null) {
            config.setMedicalEmpRatio(DEFAULT_MEDICAL_RATE);
        }
        if (config.getUnemploymentEmpRatio() == null) {
            config.setUnemploymentEmpRatio(DEFAULT_UNEMPLOYMENT_RATE);
        }
        if (config.getFundEmpRatio() == null) {
            config.setFundEmpRatio(DEFAULT_FUND_RATE);
        }
        if (config.getCalcBaseMin() == null) {
            config.setCalcBaseMin(DEFAULT_BASE_MIN);
        }
        if (config.getCalcBaseMax() == null) {
            config.setCalcBaseMax(DEFAULT_BASE_MAX);
        }
    }

    private boolean isLegacyCompanyStyle(SocialSecurityConfig config) {
        BigDecimal pension = valueOrZero(config.getPensionEmpRatio());
        BigDecimal medical = valueOrZero(config.getMedicalEmpRatio());
        BigDecimal unemployment = valueOrZero(config.getUnemploymentEmpRatio());
        return pension.compareTo(new BigDecimal("0.12")) > 0
                || medical.compareTo(new BigDecimal("0.05")) > 0
                || unemployment.compareTo(new BigDecimal("0.01")) > 0;
    }

    private SocialSecurityConfig buildDefaultPersonalConfig(LocalDate effectiveDate) {
        SocialSecurityConfig config = new SocialSecurityConfig();
        config.setConfigName("系统默认个人社保比例");
        config.setPensionEmpRatio(DEFAULT_PENSION_RATE);
        config.setMedicalEmpRatio(DEFAULT_MEDICAL_RATE);
        config.setUnemploymentEmpRatio(DEFAULT_UNEMPLOYMENT_RATE);
        config.setFundEmpRatio(DEFAULT_FUND_RATE);
        config.setCalcBaseMin(DEFAULT_BASE_MIN);
        config.setCalcBaseMax(DEFAULT_BASE_MAX);
        config.setEffectiveDate(effectiveDate != null ? effectiveDate : LocalDate.now());
        config.setIsActive(1);
        return config;
    }

    private BigDecimal toRatioDecimal(BigDecimal percentValue) {
        BigDecimal value = valueOrZero(percentValue);
        return value.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
