package com.salary.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@HeadFontStyle(fontHeightInPoints = 11)
@ColumnWidth(15)
public class EmployeeExportVO {

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("身份证号")
    @ColumnWidth(20)
    private String idCard;

    @ExcelProperty("手机")
    private String phone;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("岗位")
    private String positionName;

    @ExcelProperty("入职日期")
    private LocalDate hireDate;

    @ExcelProperty("基本工资")
    private BigDecimal baseSalary;

    @ExcelProperty("上级经理")
    private String managerName;

    @ExcelProperty("状态")
    private String statusStr;

    @ExcelProperty("银行卡号")
    @ColumnWidth(25)
    private String bankAccount;

    @ExcelProperty("开户行")
    @ColumnWidth(20)
    private String bankName;

    @ExcelProperty("头像")
    @ColumnWidth(10)
    private String avatar;

}
