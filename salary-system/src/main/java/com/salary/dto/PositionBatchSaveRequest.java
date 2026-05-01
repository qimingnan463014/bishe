package com.salary.dto;

import com.salary.entity.Position;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PositionBatchSaveRequest {

    private Long deptId;

    private List<Position> positions = new ArrayList<>();
}
