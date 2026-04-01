# 员工薪资管理系统 - 开发进度总结报告

本文档整理了项目从初期混乱状态到目前“论文演示就绪”状态的所有关键修改、架构决策及当前状态，旨在为其他协作 AI 提供完整的技术上下文。

---

## 1. 项目背景与目标
- **技术栈**：Java SpringBoot + MyBatis Plus + MySQL + Vue.js + Element Plus.
- **核心目标**：构建一个稳定、美观、且符合毕业设计演示需求的员工薪资管理系统。
- **演示背景**：需要支持管理员（Admin）、部门经理（Manager）和普通员工（Employee）三种角色的权限隔离，且数据展示需清晰直观。

---

## 2. 数据库架构演进 (Database Evolution)
数据库经历了从无法启动到 V11.0 终极版的演进，解决了大量底层兼容性问题。

### 核心修复内容：
- **全表重构**：恢复了原始设计中的 **18 张表**（涵盖考勤、社保、工资项、计算结构、计算记录、审批、公告等）。
- **SQL 语法加固**：
    - **反引号转义**：为所有表名和字段名包裹反引号 (`` ` ``)，彻底解决了 `year_month` 等 MySQL 保留关键字导致的 `1064` 语法错误。
    - **字段补全**：补全了 `t_user` (avatar, email, last_login_ip), `t_department` (description, sort_order, update_time), `t_employee` (phone, id_card) 等 20 余个缺失字段。
- **演示数据**：预置了 5 名经理（10001-10005）与 10 名员工（001-010），所有初始密码统一为 `123456`。

---

## 3. 后端架构逻辑优化 (Backend Fixes)

### 角色数据隔离 (RBAC Isolation)
- **底层 SQL 过滤**：由于之前的列表数据混杂，我们在 `EmployeeMapper.xml` 中引入了基于 `u.role` 的硬过滤：
    - `selectPageWithDetails`：强制 `WHERE u.role = 3`，仅显示普通员工。
    - `selectPageWithDetailsForManager`：强制 `WHERE u.role = 2`，仅显示部门经理。
- **Mapper 增强**：在查询中显式关联 `t_user` 表，提取 `u.role` 到 `Employee` 实体中，供前端进行更精细的逻辑判断。

### 统计图表适配 (Dashboard Charts)
- **别名对齐**：修复了 `SalaryRecordMapper` 和 `EmployeeMapper` 中多个统计查询的别名冲突（如将 `name` 统一改为 `dept_name` 或 `month`），确保前端 ECharts 获取到的 JSON 属性与渲染逻辑匹配，消除了 `undefined` 导致的图表崩坏。

### 实体类映射修复
- **银行卡字段**：由于数据库 `bank_account` 与前端 `bankCard` 的字段差异，在 `Employee.java` 内部通过 Getter/Setter 建立了逻辑映射，避免了 MyBatis 的直接冲突。

---

## 4. 前端 UI 增强 (Frontend Improvements)

### 管理后台 (Admin Dashboard)
- **列表显示增强**：在员工和经理列表中，新增了“登录账号”、“初始密码”、“银行卡号”和“直属经理”等演示友好型列。
- **列表分离**：前端 `index.html` 对应后端接口，实现了“普通员工档案”与“部门经理档案”两个菜单的物理分离展示。
- **仪表盘修复**：首页“部门人数分布”和“各部门人均薪资”图表已恢复动态加载能力。

---

## 5. 当前项目状态 (Current Status)

| 模块 | 状态 | 说明 |
| :--- | :--- | :--- |
| **数据库** | 🟢 已稳定 (V11.0) | 支持一键 `reset_data.sql` 重置，无报错。 |
| **基础档案** | 🟢 已完成 | 部门、经理、员工档案增删改查逻辑完整。 |
| **考勤管理** | 🟡 待深度验证 | 基础记录可见，复杂异常逻辑待测。 |
| **薪资核算** | 🟢 已优化 | 结构记录、计算记录别名已修复，图表展示正常。 |
| **权限控制** | 🟢 已完成 | Admin、Manager、Employee 三级隔离逻辑已在 SQL 层加固。 |

---

## 6. 未实现/待优化项 (Pending / Backlog)
1. **自动化算薪逻辑**：虽然库表已就绪，但复杂的考勤自动扣款与个税累计预扣逻辑可能仍需在 Service 层深度联调。
2. **文件导入导出**：EasyExcel 的批量导入模板由于数据库字段频繁变动，可能需要重新生成以匹配 V11.0 架构。
3. **系统日志记录**：`t_sys_log` 虽然建表完成，但切面逻辑（AOP）是否覆盖所有核心操作仍待确认。

---

## 7. 给其他 AI 的建议 (Instructions for Other AIs)
- **优先读取**：[reset_data.sql](file:///e:/bishe/salary-system/reset_data.sql) 是系统的“根基”，任何表结构变动必须在此同步。
- **Mapper 规范**：修改 SQL 时务必包裹反引号，并检查 `EmployeeMapper.xml` 里的 `u.role` 过滤条件。
- **字段映射**：遇到 UI 字段名与 DB 字段名不一致（如 bankCard vs bank_account），优先在实体类 Java 代码中处理，不要随意改动 ResultMap 重复映射。
