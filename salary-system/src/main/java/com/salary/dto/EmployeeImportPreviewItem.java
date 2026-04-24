package com.salary.dto;

import lombok.Data;

@Data
public class EmployeeImportPreviewItem {

    private Integer rowNo;

    private String empNo;

    private String realName;

    private String genderStr;

    private String phone;

    private String idCard;

    private String deptName;

    private String positionName;

    private String hireDate;

    private String bankAccount;

    private String bankName;

    private String roleStr;

    private String managerNo;

    private Boolean existing;

    private Boolean updateExisting;

    private Boolean importable;

    private String message;

    private String currentSummary;
}
