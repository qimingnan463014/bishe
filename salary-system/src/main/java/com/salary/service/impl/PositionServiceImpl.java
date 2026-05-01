package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.entity.Employee;
import com.salary.entity.Position;
import com.salary.mapper.EmployeeMapper;
import com.salary.mapper.PositionMapper;
import com.salary.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    private final PositionMapper positionMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public List<Position> listByDeptId(Long deptId) {
        if (deptId == null) {
            return java.util.Collections.emptyList();
        }
        return positionMapper.selectByDeptId(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncByDept(Long deptId, List<Position> positions) {
        if (deptId == null) {
            throw new RuntimeException("部门编号不能为空");
        }
        List<Position> incoming = positions == null ? java.util.Collections.emptyList() : positions;
        validatePositions(incoming);

        List<Position> existing = positionMapper.selectList(
                new LambdaQueryWrapper<Position>().eq(Position::getDeptId, deptId));
        Set<Long> retainedIds = incoming.stream()
                .map(Position::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> deletingIds = existing.stream()
                .map(Position::getId)
                .filter(id -> !retainedIds.contains(id))
                .collect(Collectors.toList());
        ensureManagerPositionNotDeleted(existing, deletingIds);
        ensureNoEmployeeUsesPositions(deletingIds);

        for (Position row : incoming) {
            row.setDeptId(deptId);
            if (row.getSalaryMin() == null) {
                row.setSalaryMin(BigDecimal.ZERO);
            }
            if (row.getSalaryMax() == null) {
                row.setSalaryMax(BigDecimal.ZERO);
            }
            if (row.getStatus() == null) {
                row.setStatus(1);
            }
            if (row.getId() == null) {
                positionMapper.insert(row);
            } else {
                Position before = positionMapper.selectById(row.getId());
                if (before == null || !deptId.equals(before.getDeptId())) {
                    throw new RuntimeException("岗位不存在或不属于当前部门");
                }
                row.setIsManagerPosition(before.getIsManagerPosition());
                positionMapper.updateById(row);
            }
        }

        if (!deletingIds.isEmpty()) {
            deletingIds.forEach(positionMapper::deleteById);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Position ensureManagerPosition(Long deptId) {
        if (deptId == null) {
            throw new RuntimeException("部门编号不能为空");
        }
        Position existing = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getDeptId, deptId)
                .eq(Position::getIsManagerPosition, 1)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Position created = new Position();
        created.setDeptId(deptId);
        created.setPositionName("部门经理");
        created.setSalaryMin(BigDecimal.ZERO);
        created.setSalaryMax(BigDecimal.ZERO);
        created.setDescription("系统自动生成的经理岗位，可改名，不可删除");
        created.setStatus(1);
        created.setIsManagerPosition(1);
        positionMapper.insert(created);
        return created;
    }

    private void validatePositions(List<Position> positions) {
        Set<String> names = new HashSet<>();
        for (Position row : positions) {
            String name = row == null ? null : row.getPositionName();
            if (!StringUtils.hasText(name)) {
                throw new RuntimeException("岗位名称不能为空");
            }
            String normalized = name.trim();
            if (!names.add(normalized)) {
                throw new RuntimeException("存在重复岗位名称：" + normalized);
            }
            if (row != null) {
                row.setPositionName(normalized);
                row.setDescription(StringUtils.hasText(row.getDescription()) ? row.getDescription().trim() : null);
            }
        }
    }

    private void ensureNoEmployeeUsesPositions(List<Long> deletingIds) {
        if (deletingIds == null || deletingIds.isEmpty()) {
            return;
        }
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().in(Employee::getPositionId, deletingIds));
        if (count != null && count > 0) {
                throw new RuntimeException("存在员工仍在使用将删除的岗位，请先调整员工岗位");
        }
    }

    private void ensureManagerPositionNotDeleted(List<Position> existing, List<Long> deletingIds) {
        if (existing == null || existing.isEmpty() || deletingIds == null || deletingIds.isEmpty()) {
            return;
        }
        boolean deletingProtected = existing.stream().anyMatch(row ->
                deletingIds.contains(row.getId()) && Objects.equals(row.getIsManagerPosition(), 1));
        if (deletingProtected) {
            throw new RuntimeException("部门经理岗位为系统保留岗位，不允许删除");
        }
    }
}
