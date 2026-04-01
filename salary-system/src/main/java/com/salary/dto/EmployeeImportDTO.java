package com.salary.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

/**
 * 员工 Excel 导入 DTO
 * Excel 列顺序与此字段顺序对应
 */
@Data
public class EmployeeImportDTO {

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("性别(男/女)")
    private String genderStr;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("身份证号")
    private String idCard;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("岗位名称")
    private String positionName;

    @ExcelProperty("入职日期(yyyy-MM-dd)")
    @DateTimeFormat("yyyy-MM-dd")
    private String hireDate;

    @ExcelProperty("基本工资")
    private String baseSalary;

    @ExcelProperty("银行卡号")
    private String bankAccount;

    @ExcelProperty("开户行")
    private String bankName;

    /**
     * 角色：填 "经理" 则创建经理账号(role=2)，默认员工(role=3)
     */
    @ExcelProperty("角色(员工/经理)")
    private String roleStr;

    @ExcelProperty("所属经理工号")
    private String managerNo;
}
