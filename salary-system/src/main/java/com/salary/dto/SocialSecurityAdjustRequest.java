package com.salary.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SocialSecurityAdjustRequest {
    private BigDecimal pensionRate;
    private BigDecimal medicalRate;
    private BigDecimal unemploymentRate;
    private BigDecimal fundRate;
    private String effectiveMonth;
}
