# 演示数据重建最小清单

## 目标
- 保留系统配置与页面展示基础数据。
- 定向重建演示业务数据，不走全删库。
- 配合 [`员工导入模板.xlsx`](/E:/bishe/员工导入模板.xlsx) 和 [`考勤打卡导入模板.xlsx`](/E:/bishe/考勤打卡导入模板.xlsx) 走一条顺的答辩演示链路。

## 建议保留
- `t_department`
- `t_position`
- `t_attendance_rule`
- `t_salary_structure`
- `t_social_security_config`
- `t_announcement`
- 静态资源目录：`salary-system/src/main/resources/static/front_assets`

## 建议定向重建
- `t_user`
- `t_employee`
- `t_attendance_record`
- `t_attendance_apply`
- `t_performance`
- `t_salary_record`
- `t_salary_payment`
- `t_tax_accumulate`
- `t_salary_feedback`
- `t_anomaly_report`

## 可按需清理
- `t_sys_log`

## 说明
- 当前库里没有 `t_sys_config`，演示所需的系统展示内容主要落在公告表和静态资源目录，不要按旧想象去清不存在的表。
- 员工导入时，`角色(员工/经理)` 填“经理”才会创建经理账号；普通员工填“员工”。
- 员工导入时，`所属经理工号` 只有当该经理也在同一份 Excel 中出现时，绑定才会成功。
- 考勤打卡导入接口除 Excel 文件外，还要额外传 `yearMonth=YYYY-MM`。

## 推荐演示顺序
1. 先保留配置类表与图片资源。
2. 清空并重建业务类表。
3. 导入 [`员工导入模板.xlsx`](/E:/bishe/员工导入模板.xlsx)。
4. 导入 [`考勤打卡导入模板.xlsx`](/E:/bishe/考勤打卡导入模板.xlsx) 并传对应月份。
5. 补少量绩效、反馈或申请数据。
6. 用经理端生成工资草稿。
7. 提交审核、管理员审核、发放，完成整条演示链路。
