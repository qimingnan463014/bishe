package com.salary.dto;

import lombok.Data;

@Data
public class EmployeeImportExecuteResult {

    private Integer totalRows = 0;

    private Integer insertedRows = 0;

    private Integer updatedRows = 0;

    private Integer skippedRows = 0;
}
