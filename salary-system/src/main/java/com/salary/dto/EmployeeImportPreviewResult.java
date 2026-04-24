package com.salary.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeImportPreviewResult {

    private Integer totalRows = 0;

    private Integer newRows = 0;

    private Integer existingRows = 0;

    private Integer importableRows = 0;

    private Integer invalidRows = 0;

    private List<EmployeeImportPreviewItem> items = new ArrayList<>();
}
