# CHANGELOG

## 2026-04-01 仓库卫生：根目录视频不入库

### Git / GitHub
- 根目录下的演示/录屏类视频（`*.mp4` 等）不再纳入版本控制，并在 `.gitignore` 中按根路径忽略，避免误提交与超过 GitHub 单文件约 100MB 限制。
- 本地仓库 `user.email` 已与 GitHub 主邮箱对齐为 `614377781@qq.com`（便于提交记录与账户关联）。

---

## 2026-04-01 V13.2 (工资个税界面修复与弹窗美化)

### 全局底层修复：MySQL 关键字 `year_month` 转义防御
**修改**
- [后端] 解决全量统计/分页接口中因 `year_month` 是 MySQL 保留关键字引发的 `SQLSyntaxErrorException` 500 报错。
- [后端] 为 `SalaryRecord`, `Performance`, `AttendanceRecord`, `SalaryPayment`, `TaxAccumulate` 等所有含该字段的实体强制添加 `@TableField("\`year_month\`")` 显式转义映射。
- [后端] 扫描并替换了 `src/main/resources/mapper/` 目录下所有 XML 映射文件中手写的 `year_month` 为带有反引号的格式。

### 工资个税与社保界面三项修复
**修改**
- [前端] 将 `tax` 表格的 `table-card` 改为 `compact-table-card`，与员工/部门经理等宽表保持一致。
- [前端] 重写 `taxDisplayRows` 中经理信息回溯逻辑：原来仅依靠 `allEmployees.userId` 匹配（经理未在 allEmployees），改为三级多源回溯策略（行自带字段 → deptManagers 列表按 deptId/empNo 查 → departments.managerId + deptManagers 联查），彻底修复部门经理与账号显示 `-` 的问题。
- [前端] 新增全局弹窗美化样式体系（`.modern-dialog`、`.dialog-header-gradient`、`.detail-card-grid`、`.detail-item`），让弹窗渐变 Header + 卡片 Grid 布局取代原来的 `el-descriptions`，同时修改弹窗改为双栏 Row:gutter 对齐格式，整体视觉更简洁专业。

---

## 2026-04-01 V13.5 (核心财务核算逻辑与论文双向对齐)

### 全新四重累加分绩效算法落地
**背景**：论文需求要求将各项打分具体化、金额配置化，并废弃“平均分算法”。
- **后端架构升级 (`PerformanceServiceImpl.java` & `Performance.java`)**：
  - 复用了冗余字段 `bonusDeduct` 作为【员工考勤分 (0-30)】。
  - 重写了 `autoCalcScoreAndGrade` 方法。最新记分公式：`工作表现(50) + 业务技能(10) + 工作态度(10) + 员工考勤(30) = 总得分(100)`。
  - 新增 Transient 承载字段 `qualifiedBonus`、`goodBonus`、`excellentBonus`，用于接收前端管理员自定义的奖金数额。
  - 评级区间从硬编码系数改为阶梯：`<61 (不合格/0元)`、`61-75 (合格)`、`76-85 (良好)`、`86-100 (优秀)`。
  - 极其巧妙的无感字段复用：将映射得出的最终【绝对金额】存入原本用于存比例的 `perfBonusRatio` 字段。
- **算薪直连提取 (`SalaryServiceImpl.java`)**：
  - 核心算薪函数 `calculateSalary` 删除原有的 `部门基数 × 20% × 绩效系数` 绕弯逻辑，直接读取 `perfBonusRatio` 并赋值为当月绩效奖金 `perfBonus`。
- **UI 控制塔开放 (`admin/index.html`)**：
  - 在【绩效评分】弹窗中暴露配置项输入框（默认900, 1200, 1500），评分即录入，所见即所得。

### 五险一金正向基数切片算法落地
**背景**：原版展示错位，甚至前端尝试逆向推算，不符合系统严谨性。
- **前端重算渲染 (`admin/index.html`)**：
  - 重构了在个税界面的推算能力。所有险种严格由 `baseSalary` 提取，不再通过合计包 `socialSecurityEmp` 逆推。
  - 彻底对齐论文标准：养老保险 (8%)、医疗保险 (2%)、住房公积金 (7%)、失业保险 (0.5%)。
  - 补全了之前表格和弹窗丢失的经理姓名 (`managerName`) 与经理账号 (`managerNo`) 显示。
  - 新增了【失业保险】数据列及对应的修改项。

## 2026-04-01 V13.4 (绩效评分模块字段链路修复)

### application.yml — 致命 Bug 修复
**根因：MyBatis-Plus 全局逻辑删除字段配置导致所有分页查询失败**
- 全局配置了 `logic-delete-field: deleted`，但项目中**所有数据表均无 `deleted` 列**。
- MyBatis-Plus 自动在每条 `SELECT` 末尾追加 `WHERE deleted = 0`，造成 SQL 报错 `Unknown column 'deleted'`，接口返回 500 并静默失败，前端表格显示"无数据"。
- **修复：删除 `application.yml` 中 `logic-delete-field/value/not-delete-value` 三行配置。** 重启后端服务即可。
- **影响范围**：绩效评分、考勤、薪资核算、投诉反馈等所有使用 MyBatis-Plus `selectPage` 的接口均受益。

### 实体类映射 — SQL 语法错误致命修复
**根因：MySQL 8 保留关键字冲突**
- MySQL 8.0 将 `year_month` 设为保留关键字（用于 `INTERVAL 1 YEAR_MONTH` 等）。
- MyBatis-Plus 直接生成 `SELECT id, year_month FROM ...` 导致报 500 `java.sql.SQLSyntaxErrorException`（被前端拦截为静默失败显示无数据）。
- **修复**：对涉及该字段的 **7 个实体类**（`Performance`、`SalaryRecord`、`AttendanceRecord`、`SalaryFeedback`、`SalaryPayment`、`TaxAccumulate`、`AnomalyReport`）统一使用 `@TableField("\`year_month\`")` 进行反引号转义。

### admin/index.html
**Bug修复：绩效评分页面数据显示空白**
- **根因**：前端 `performanceDisplayRows` 映射和表格 `prop` 使用的字段名（`attendanceScore/attitudeScore/skillScore/performanceScore/bonusPenaltyScore`）与后端 `Performance.java` 实体类实际字段名（`workAttitude/businessSkill/workPerformance/bonusDeduct`）严重不匹配，导致后端返回的数据字段全部被丢弃为 `-`，表格空白。
- **修复范围**（5处全覆盖）：
  1. `performanceDisplayRows` computed 映射字段名对齐
  2. 表格 `<el-table-column>` 的 `prop` 属性全部修正
  3. `calcPerfTotal()` 计算公式修正：改为三项平均分 + 奖惩加减分（= 后端 PerformanceServiceImpl 实际逻辑）
  4. `openPerfForm()` 初始化默认值字段名修正
  5. `savePerformance()` POST/PUT 请求体字段名修正
  6. `openPerformanceDetail()` 详情弹窗字段名全部修正
- **附加**：grade tag 颜色同时兼容中文（优秀/良好/中等）和英文字母（S/A/B/C）两套旧数据格式。

---

## 2026-03-31 V13.3 (交互与导出专项修复)

### admin/index.html
**1. 薪资反馈模块功能恢复**
- 修复了“薪资反馈”菜单点击后页面出现空白的问题。原因是对应的页面区块遗漏了对 `currentMenu === 'salary-feedback'` 的判断，导致该页面组件被隐藏。

**2. 核心导出功能升级为 Excel (.xlsx)**
- 解决了导出格式问题，将全系统的 CSV 导出（包括考勤、薪资、绩效、发放等 6 处），全部依赖 `xlsx.full.min.js` 转换为标准的 Excel 文档流输出，避免了跨平台 CSV 字符乱码和格式折损。

**3. 薪资核算默认月份体验优化 (经修正为勾选行核算)**
- 修正 `batchCalcSalary` 逻辑：不再全局强推算某月，而是调整为严谨的操作流：现在点击【核算】必须提前勾选特定的列表数据，系统会对已选中的员工逐行下发核算指令，支持细颗粒度重算。

---

## 2026-03-31 V13.2

### admin/index.html

**修复：操作按钮颜色不显示**
- 全局去除按钮 `type` 属性多余反斜杠（`type=\"warning\"` → `type="warning"`）
- 影响范围：部门、部门经理、员工、通知、考勤规则、薪资结构、考勤数据、考勤申请、异常上报、投诉反馈、工资个税、绩效评分、薪资核算、薪资发放等所有操作按钮
- 现在正确显示蓝(primary)/橙(warning)/红(danger)/绿(success)色

**统一分页组件（8个模块）**
- 考勤数据、考勤申请、异常上报、投诉反馈、工资个税、绩效评分、薪资核算、薪资发放
- 全部改为 `v-model:current-page` + `v-model:page-size` + `@size-change`，layout 统一 `"total, sizes, prev, pager, next, jumper"`
- 将 8 个 `xxxSize` refs 加入 setup() return，可在模板中访问

---

## 2026-03-31 (hotfix)

### 白屏修复：`p` 变量未定义导致 Vue 实例挂载失败 (V13.1-hotfix)
**根因**
- V13.1 全局 UI 重构时，`setup()` 的 `return { p, userInfo, ... }` 里引用了 `p`，但忘记在函数体内声明 `const p = reactive({})`。
- 这导致 JS 运行时抛出 `ReferenceError: p is not defined`，整个 `setup()` 崩溃，Vue 实例无法挂载，页面仅剩 `body` 浅蓝背景色。

**修复**
- `admin/index.html` L2608：在 `setup()` 顶部补充声明 `const p = reactive({});`。

**第二轮 hotfix**
- `p` reactive 对象改为包含所有模板用到的分页子对象（`departments / deptManagers / carousel / systemIntro / systemLogs`），每个初始化为 `{ current:1, size:10 }`，修复 `Cannot read properties of undefined (reading 'current')` 渲染崩溃。
- 补充 `openSinglePaymentGateway(row)` 单笔发放弹窗函数，并加入 `return` 暴露给模板，修复薪资发放页点击【发放】按钮报 `is not a function` 的问题。

---

## 2026-03-31

### 全局 UI 与表格组件重构 (V13.1)
**修改与新增**
- [前端] 统一规范了全站所有 `<el-table>` 内部的 `<el-button>` 按钮色值体系，强制规范为：详情/新增/普通操作(`primary`)、修改/编辑(`warning`)、删除/解散/驳回(`danger`)、审核/下发/通过(`success`)。
- [前端] 针对原来缺失分页组件的数据表格（部门、部门经理、轮播图管理、系统简介、系统日志），使用 Vue3 的 `reactive` 重构注入了统一的纯前端分页切片能力 `p` 并挂载对应 `<el-pagination>`。

### 基建外挂扩充：开发规范技能库挂载
**新增**
- [规范] 挂载 `frontend-design-sop.md` 技能库：提取 Anthropic 最佳实践，定义 Vue3 + Element Plus 的现代理念、色彩、间距等核心 UI 设计避坑体系。
- [规范] 挂载 `office-docs-sop.md` 技能库：打通 Java 报表导出（EasyExcel / POI-TL / iText）与前端 Vue3 Blob 流安全截获处理标准。

### 薪资发放流程闭环上线 (V13.0)
**新增**
- [前端] 完善 `admin/index.html`，新增 `auditSalaryRecord` (单笔二审)、`batchAuditSalary` (批量二审)以及对应的 UI 按钮。在薪资列表中加入 `calcStatus` 页面显示。
- [前端] 新增针对薪资发放的聚合支付交互 (`paymentGatewayVisible`、`paymentProcessing`)，支持网银、支付宝企业版、微信商户代发特效选择。
- [后端] `SalaryController` 新增 `/audit` (单笔) 和 `/batch-audit` (批量) 接口。
- [后端] 修改 `SalaryServiceImpl` 中的 `paySalary` 接口，严格拦截至只有 `calcStatus == 3` (已审核) 的记录才可发放，彻底闭环资金安全漏洞。

**状态机制**
- 完善了从 "草稿(1) -> 已发布待审(2) -> 已审核(3) -> 已发放(4)" 的不可逆状态流转门控。

### 迁移记录（来自原计划文件）
- 迁移来源：`管理员界面差距修复跟踪.md` 的“变更记录”段落。
- 新建跟踪文件：`管理员界面差距修复跟踪.md`
- 写入当前发现的差异清单（1~5 项暂未完成修复）
- 约定：后续用于分析/调试的临时脚本或文件（例如一次性视频抽帧脚本）使用完毕后必须删除，不长期留在仓库里，避免污染项目结构。

## 2026-03-30

### 问题 1：部门信息界面职位工资无法保存和显示
**原因**
- 数据库 `t_department` 表缺少 `position_salary` 字段。
- `Department.java` 实体类缺少 `positionSalary` 字段。
- `DepartmentServiceImpl.java` 没有处理 `positionSalary` 的默认值。
- 前端表单和表格缺少职位工资相关字段。

**修复**
- 数据库：在 `t_department` 表添加 `position_salary` 字段（`DECIMAL(10,2)`，默认 `0.00`）。
- 后端：修改 `Department.java`，添加 `positionSalary` 字段。
- 后端：修改 `DepartmentServiceImpl.java`，添加 `positionSalary` 为空时的默认值处理。
- 前端：修改部门信息表格，添加“职位工资”列。
- 前端：修改部门表单，添加职位工资输入框。
- 前端：修改 `deptForm` 和 `openDeptForm`，支持 `positionSalary`。
- 前端：修改 `managerDisplayRows`，经理基本工资 = 部门基本工资 + 部门职位工资。
- 前端：修改 `employeeDisplayRows`，员工基本工资 = 部门基本工资。

### 问题 2：考勤数据不显示
**原因**
- `year_month` 是 MySQL 保留字，在 SQL 查询中需要用反引号包裹。

**修复**
- 修改 `AttendanceRecordMapper.xml`，给 `year_month` 加上反引号。

### 问题 3：薪资核算不显示
**原因**
- `year_month` 是 MySQL 保留字，在 SQL 查询中需要用反引号包裹。

**修复**
- 修改 `SalaryRecordMapper.xml`，给 `year_month` 加上反引号。

### 问题 4：绩效评分查询可能有问题
**原因**
- `year_month` 是 MySQL 保留字，在 SQL 查询中需要用反引号包裹。

**修复**
- 修改 `PerformanceMapper.xml`，给 `year_month` 加上反引号。

### 问题 5：个税累计查询可能有问题
**原因**
- `year_month` 是 MySQL 保留字，在 SQL 查询中需要用反引号包裹。

**修复**
- 修改 `TaxAccumulateMapper.xml`，给 `year_month` 加上反引号。

### 问题 6：薪资反馈不显示（待修复，先放一边）
**原因**
- `SalaryFeedback.java` 实体类缺少 `empNo`、`empName`、`replyUserName` 字段。
- 虽然数据库中有这些字段，但实体类缺少，无法正确映射。

**修复**
- 修改 `SalaryFeedback.java`，添加 `empNo`、`empName`、`replyUserName` 字段。
- 注意：此修复可能不完整，待后续进一步排查。

### 问题 7：薪资发放不显示
**原因**
- 前端调用了错误的接口 `/api/payment/page`，应调用 `/api/salary/page`。
- 前端参数名传错：`paymentSearch.calcStatus` 应为 `paymentSearch.payStatus`。
- 缺少工号、姓名、部门 ID 等搜索参数传递。

**修复**
- 修改前端 `paymentSearch`，将 `calcStatus` 改为 `payStatus`。
- 修改前端搜索框绑定，将 `v-model="paymentSearch.calcStatus"` 改为 `v-model="paymentSearch.payStatus"`。
- 修改 `loadPayments` 函数，改用 `/api/salary/page` 接口。
- 在 `loadPayments` 中补充搜索参数：`empNo`、`empName`、`realName`、`deptId`、`calcStatus`。

### 新需求记录（待实现）
- 点击发放后，不应该直接弹窗确认，而应该跳转到一个支付界面。
- 支付界面显示银行和支付宝、微信选项。
- 点击支付后显示支付成功提示。

### 问题 8：工资个税与社保不显示
**原因**
- 前端调用了错误接口 `/api/tax/page`，应调用 `/api/salary/page`。
- `taxDisplayRows` 计算属性中缺少一些字段默认值处理。
- `deductDate` 字段 fallback 值不正确。

**修复**
- 修改 `loadTaxRecords` 函数，改用 `/api/salary/page` 接口。
- 在 `loadTaxRecords` 中补充搜索参数：`empNo`、`empName`、`realName`、`deptId`。
- 修改 `taxDisplayRows` 计算属性，将 `deductDate` 的 fallback 从 `row.deductDate` 改为 `row.recordDate || row.payDate || row.createTime`。

### 数据插入记录
- 绩效评分数据：15 条
- 薪资记录数据：15 条
- 公告数据：5 条
- 考勤申请数据：5 条
- 异常上报数据：5 条
- 薪资反馈数据：5 条
- 薪资发放数据：15 条
- 个税累计数据：10 条
- 社保配置数据：2 条
- 考勤记录数据：15 条

**合计：92 条数据**

