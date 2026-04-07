# CHANGELOG

## 2026-04-07 V13.87（首页网站配置同步口径与关于我们白屏收口）
### salary-system/src/main/java/com/salary/service/impl/SysConfigServiceImpl.java
- [后端] 将系统配置持久化从依赖缺失的 `t_sys_config` 数据表改为 `uploads/sys-config/sys-config.json` 文件存储，修复 `/api/sys-config/website_content` 因表不存在而持续报错的问题。
- [后端] `getByConfigKey/saveOrUpdateByKey` 改为线程安全的文件读写实现，管理员保存首页配置后，员工端与管理端都能读取同一份服务端配置。
### salary-system/src/main/resources/static/home.html
- [前端] 修正首页“门户面板”缺失闭合标签的问题，避免“通知公告 / 关于我们详情 / 个人中心”等页面被错误嵌套在首页面板中；点击“关于我们-查看详情”不再进入空白页。
- [前端] 首页网站配置加载改为只信任服务端配置，服务端缺失时统一回退默认值，不再继续读取旧 localStorage 导致员工端和管理端显示不同步。
- [前端] 为 `通知公告 / 关于我们 / 系统简介` 标题补齐空字符串兜底，修复首页“关于我们”标题被空配置覆盖后不显示的问题。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理端网站配置加载补齐标题/正文/图片的空值归一化，避免历史空字符串配置继续把区块标题渲染没掉。
- [前端] 当服务端配置为空但浏览器本地仍有旧版首页配置时，管理端会自动将本地配置补同步回服务端，帮助恢复轮播图、关于我们、系统简介与员工首页的同步。
- [前端] 网站配置保存提示文案从“同步到数据库”调整为“同步到系统配置”，与新的服务端持久化实现保持一致。

## 2026-04-07 V13.86（关于我们详情空白页修复）
### salary-system/src/main/resources/static/home.html
- [修复] 将“关于我们详情页”图片列表从组件自闭合渲染改为稳定的 `img` 列表渲染，修复点击“查看详情”后出现空白页的问题。

## 2026-04-07 V13.85（首页顺序重排与关于我们详情修复）
### salary-system/src/main/resources/static/home.html
- [前端] 将员工首页内容顺序调整为“轮播图 -> 通知公告 -> 关于我们 -> 系统简介”，把系统简介移动到页面最下方，并保留首页公告卡片在轮播图下方优先展示。
- [前端] 修复首页“关于我们”图片兜底逻辑：数据库配置缺项时会按默认图片回填，不再出现后台记录已有 3 张图但首页图片区为空的情况。
- [前端] 新增员工端“关于我们详情页”，首页“关于我们”卡片支持点击或“查看详情”进入独立详情界面。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理端“关于我们”列表页补齐“详情”按钮，支持直接打开只读详情弹窗，不必只能进入编辑模式查看内容。

## 2026-04-07 V13.84（管理端网站配置同步与备份权限收口）
### salary-system/src/main/resources/static/admin/index.html
- [权限] 右上角用户下拉中的“数据备份”改为仅 `role=1` 系统管理员可见，并在命令处理函数中增加二次校验，经理即使手动触发也会被拒绝。
- [前端] 将管理端网站配置默认值改为与员工首页一致：统一门户标题、轮播图默认图片、关于我们默认图片、系统简介默认图片与文案。
- [前端] `loadWebsiteContent()` 改为与员工首页同口径的图片兜底逻辑，数据库配置缺项时会自动回填默认轮播图和系统简介图片，不再出现首页有图而管理端显示“共 0 张 / 配图 -”。
- [前端] 补齐 `aboutEditForm.subtitle` 回填，避免“关于我们”编辑弹窗打开后副标题丢失。

## 2026-04-07 V13.83（员工端图标注册补齐）
### salary-system/src/main/resources/static/home.html
- [修复] 员工端应用初始化补齐 `ElementPlusIconsVue` 全量注册，修复 `修改个人信息` 弹窗头部 `<User />` 图标未渲染、只剩蓝色底块的问题。
- [说明] 本次问题根因是前端页面未注册图标组件，和后端重启无关。

## 2026-04-07 V13.82（员工个人信息弹窗完全对齐经理端样式）
### salary-system/src/main/resources/static/home.html
- [前端] 继续按“直接复用经理端布局代码”的方式收口员工个人信息弹窗，保留员工端自己的字段值与保存逻辑，不再单独做一套视觉实现。
- [前端] 将经理页同款 `detail-form / detail-form-tip` 样式同步到员工端，补齐表单标签字重、间距、输入框圆角等细节，修复图标、字体与输入区观感不一致的问题。

## 2026-04-07 V13.81（home.html UTF-8 回退并定点重做）
### salary-system/src/main/resources/static/home.html
- [修复] 将因编码污染而整页中文乱码的 `home.html` 回退到 Git 基线，恢复正常中文文案，避免继续在脏文件上叠加修改。
- [前端] 在恢复后的干净版本上，重新收口员工个人中心资料卡：宽度改为 `720px`，标题、按钮、字段标签与经理页统一。
- [前端] 重新以 UTF-8 定点改造员工“修改个人信息”弹窗，保留 `身份证号` 编辑链路，并新增 `openEmpEditForm()` 先回填再打开弹窗。

## 2026-04-07 V13.80（员工与经理个人信息界面统一）
### salary-system/src/main/resources/static/home.html
- [前端] 收口员工个人中心资料卡的标题、按钮、标签与字段文案，清理该区域残留乱码，统一为与经理页一致的视觉表达。
- [前端] 新增 `openEmpEditForm()`，点击“修改”时先按当前档案同步表单，再打开弹窗，避免编辑态和展示态脱节。
- [前端] 定点修正员工个人信息保存提示文案，恢复“未获取到员工ID / 保存成功 / 保存失败”等中文提示。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 经理个人中心展示区补回 `身份证号` 与 `部门经理` 字段，保持展示口径与员工端一致。
- [前端] 将经理“修改个人信息”弹窗重构为与员工端一致的双卡片布局：上方只读信息、下方个人资料，标题、副标题、按钮样式同步统一。

## 2026-04-07 V13.79（经理个人信息弹窗补齐身份证号编辑）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 在经理/管理员个人信息修改弹窗中补回 `身份证号` 输入项，保持现有 modern-dialog 样式不变。
- [前端] 同步补齐 `selfInfo / selfEditForm / loadSelfInfo / openSelfEditForm / saveSelfInfo` 的 `idCard` 链路，避免弹窗里能看不能存或保存后页面状态不刷新的问题。

## 2026-04-07 V13.78（员工个人中心宽度与经理页对齐）
### salary-system/src/main/resources/static/home.html
- [前端] 将员工个人中心的资料卡与密码卡统一收口为同一 `720px` 宽度，修复“上宽下窄”的排版问题。
- [前端] 同步将员工头像区尺寸与资料表格列数收口为更接近经理页的布局，减少两端个人中心视觉差异。

## 2026-04-07 V13.77（经理个人中心取数纠偏与演示模板交付）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 经理/管理员个人中心 `loadSelfInfo()` 改为直接调用 `/api/auth/me`，不再误走员工分页接口，修复经理页展示成下属员工档案的问题。
- [前端] 个人中心回填字段统一按当前登录人真实档案映射 `empId / empNo / deptName / bankCard / positionName / avatar`，避免再受员工列表分页结果影响。
### DEMO_DATA_PLAN.md
- [方案] 新增演示数据重建最小清单，明确当前项目应保留 `t_department / t_position / t_attendance_rule / t_salary_structure / t_social_security_config / t_announcement` 等配置类数据，并将员工、考勤、薪资、绩效、反馈等业务表列为定向重建对象。
- [方案] 额外注明当前库中不存在 `t_sys_config`，防止后续按错误表名清库。
### 员工导入模板.xlsx / 考勤打卡导入模板.xlsx
- [交付] 按 `minimax-xlsx` XML 模板流程生成两份可直接导入的 Excel 成品文件，均已通过 `formula_check.py` 静态校验。
- [模板] 员工导入模板内置经理/员工示例行与真实表头；考勤打卡导入模板内置多天打卡样例，便于直接演示导入链路。



## 2026-04-07 V13.76（经理页工资草稿生成与编辑闭环）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 经理“薪资核算”页保留原有“核算”按钮，同时新增“生成草稿”“添加草稿”两个入口；前者按所选月份批量生成经理本人及名下员工的工资草稿，后者用于单独补生成单条草稿。
- [前端] 将行内“修改(草稿)”改为“编辑草稿”，新增工资草稿编辑弹窗，支持经理在提交审核前调整 `津贴 / 其他扣款 / 备注`，并将保存成功提示统一改为“工资草稿已保存”。
- [前端] “提交审核”按钮取消浅色态，提升文字与底色对比度，避免绿色按钮文字发虚不明显。
- [修复] 删除同一 `setup` 作用域里重复声明的 `getCurrentYearMonth()`，修复浏览器 `Identifier 'getCurrentYearMonth' has already been declared` 导致整页脚本中断的问题。
### salary-system/src/main/java/com/salary/controller/SalaryController.java
- [后端] 经理调用 `/api/salary/batch-calculate` 时，批量生成范围改为“当前经理本人 + 名下员工”，解决经理自己的工资单没有入口生成的问题。
### salary-system/src/main/resources/mapper/SalaryRecordMapper.xml
- [后端] 经理查看薪资核算列表时，查询范围改为“`manager_id = 当前经理` 或 `emp_no = 当前登录经理账号`”，确保经理本人的工资草稿生成后也能在经理页看见。

## 2026-04-07 V13.75（员工头像补入与经理头像唯一化）
### salary-system/src/main/resources/static/front_assets
- [资源整理] 将 `E:\bishe\管理员界面（后台）\员工界面_files` 中的员工头像补充复制到 `front_assets`，整理出 `yuangong_touxiang1.jpg` 至 `yuangong_touxiang10.jpg` 供员工端统一引用。
- [资源修复] 补齐此前缺失的 `bumenjingli_touxiang6.jpg` 到 `front_assets`，避免数据库指向该文件时直接 404。
### 运行库 bishe.`t_user` / bishe.`t_employee`
- [数据库] 复核经理与员工头像映射后，将第 5 位部门经理的头像从重复资源改绑为 `/api/front_assets/bumenjingli_touxiang8.jpg`，保证当前 5 位经理与 10 位员工在系统中的实际展示头像均为唯一且 `t_user`、`t_employee` 双表一致。

## 2026-04-07 V13.74（front_assets 静态图片鉴权放行）
### salary-system/src/main/java/com/salary/config/SecurityConfig.java
- [后端] 将 `/front_assets/**` 加入 Spring Security 匿名放行列表，修复首页轮播图、关于我们图片及 `t_user/t_employee.avatar` 指向 `front_assets` 时被拦截为 `401` 导致整页图片“加载失败”的问题。
## 2026-04-07 V13.73（首页图片生成与全量头像真实化）
### e:\bishe\salary-system\src\main\resources\static\front_assets
- [UI设计] 统一使用大模型生成高质量的轮播图与关于我们团队宣传图，替代原有的空白或占位资源。
- [数据资源] 将 管理员界面（后台）\部门经理_files 下积累的真实人物头像整理至前端资源库 ront_assets。
- [协同开发] 前置校验头像占位符，执行 MySQL 更新，将管理端和员工端的默认头像全部动态映射至真实的本地图片路径，极大提升门户界面的可用度与美观度。

## 2026-04-07 V13.72（薪资与个税详情口径同步收口）
### salary-system/src/main/resources/static/home.html
- [前端] 员工“薪资详情”新增 `应发工资（扣款前）` 字段，并直接复用工资条构成数据计算 `grossSalary`，避免用户再看到未解释清楚的应发工资口径缺口。
- [前端] 员工“个税与社保”详情从单纯累计口径改为先展示与管理员页一致的月度扣缴明细：补齐 `基本工资 / 本月公积金 / 医疗保险 / 养老保险 / 失业保险 / 五险一金 / 本月个税 / 扣缴日期 / 经理工号 / 部门经理`，同时保留年度累计字段。
- [前端] 详情额外字段过滤继续补齐 `salaryRecordId`，修复个税详情泄露内部关联工资单主键的问题。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理员“薪资详情”新增 `应发工资（扣款前）` 字段，详情口径与员工端统一。
- [前端] 管理员薪资详情中的 `五险一金 / 考勤扣款 / 其他扣款 / 个税扣除 / 扣款合计` 统一改为负数展示，并将“经理账号”文案收口为“经理工号”，与表格和员工端保持一致。
## 2026-04-07 V13.71（员工端薪资详情口径对齐管理员端）
### salary-system/src/main/resources/static/home.html
- [前端] 员工“薪资核算”表与薪资详情补齐 `bankCard / managerNo / managerName / baseSalary` 的展示 fallback，基础工资优先按员工档案口径回填，修复员工详情与管理员详情金额不一致的问题。
- [前端] 员工薪资详情中的 `经理工号 / 部门经理` 不再直接显示 `-`，会优先从员工档案补齐；银行卡号也与管理员详情保持同一回填链路。
- [前端] 员工薪资详情额外字段过滤补齐 `slipPublishTime / createTime / updateTime / managerId`，移除截图中漏出的多余英文/内部字段。
## 2026-04-06 V13.70（员工端详情弹窗对齐管理员风格）
### salary-system/src/main/resources/static/home.html
- [前端] 将员工端旧的通用 `el-descriptions` 详情弹窗升级为管理员同款 `detail-dialog` 卡片式布局，统一头像区、标题区、标签区和详情描述区的视觉结构。
- [前端] 员工端考勤申请、薪资、薪资反馈、投诉反馈、个税社保、绩效评分 6 类详情改为按业务类型组装 `detailDialog` 数据，详情页不再直接裸循环原始对象字段。
- [前端] 在保留员工端既有中文字段映射、扣款负数展示和当前接口口径的前提下，对齐管理员端详情页体验，避免只改样式却丢字段。
## 2026-04-06 V13.69（管理员薪资表头像与银行卡列恢复）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 按用户澄清恢复管理员“薪资核算”表的 `头像 / 银行卡号` 两列，以及管理员“薪资发放”表的 `头像` 列；员工端不作回滚，继续保持个人视角的简洁表头。
- [前端] 管理员页保留上一轮已确认的字段名称同步、扣款负数口径与“加班小时”修正，本次仅回退被误删的管理员专属识别字段。
- [导出] 管理员“薪资核算”导出字段同步恢复 `银行卡号`，保持导出与页面列一致。
## 2026-04-06 V13.68（管理员薪资表同步与考勤数据加班小时收口）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理员“薪资核算”与“薪资发放”表去掉头像列，核心表头顺序收口为 `月份 / 工号 / 姓名 / 部门 / 基本工资 / 加班工资 / 绩效奖金 / 津贴 / 五险一金 / 个税 / 扣款金额 / 实发工资`，与员工端保持一致。
- [前端] 管理员薪资两张表新增/启用 `五险一金` 列，并将 `五险一金 / 个税 / 扣款金额` 统一按负数显示；相关导出字段同步调整为同一口径。
- [前端] 管理员“考勤数据”页把错误沿用的“出差时间”改回真实“加班小时”：表格、录入弹窗、详情弹窗、导出表头全部改为 `overtimeHours` 语义。
## 2026-04-06 V13.67（员工薪资表头顺序与扣款负数口径对齐）
### salary-system/src/main/resources/static/home.html
- [前端] 员工“薪资核算”表头改为 `月份 / 工号 / 姓名 / 部门 / 基本工资 / 加班工资 / 绩效奖金 / 津贴 / 五险一金 / 个税 / 扣款金额 / 实发工资 / 状态 / 操作`，与管理员薪资表的核心字段顺序保持一致。
- [前端] 个人端薪资表移除头像列与银行卡列，改为使用员工本人资料回填 `empNo / empName / deptName`，更符合个人门户视角。
- [前端] 员工端薪资表新增 `扣款金额` 列，并将 `五险一金 / 个税 / 扣款金额` 统一格式化为负数展示，和工资条扣款语义保持一致。
## 2026-04-06 V13.66（员工端所有表格操作列统一固定）
### salary-system/src/main/resources/static/home.html
- [前端] 将员工端仍未固定的“操作”列统一补齐 `fixed="right"`，现在考勤申请、薪资核算、投诉反馈等表格在横向滚动时都会始终显示右侧操作入口。
- [前端] 员工端所有带“操作”列的表格已与薪资反馈、个税社保、绩效评分页保持一致，避免用户滚动后找不到“详情 / 薪资反馈”按钮。
## 2026-04-06 V13.65（员工页全表自适应、公告详情链路与英文字段全量清理）
### salary-system/src/main/resources/static/home.html
- [前端] 员工端考勤申请、薪资核算、薪资反馈、投诉反馈、个税社保、绩效评分等所有表格列宽统一改为 `min-width` 自适应模式，表格可横向铺满卡片区域，不再依赖固定 `width`。
- [前端] 员工端首页公告卡片改为点击跳转详情页，新增“通知公告”列表页与“公告详情”页，顶部“通知公告”入口不再空白，公告数据统一按已发布状态从 `/api/announcement/list` 与 `/api/announcement/{id}` 拉取。
- [前端] 详情弹窗继续补齐员工相关实体的中文字段映射，新增覆盖 `bankAccount / sickLeaveDays / attendHours / issueFile / targetRole / loginAccount / loginPassword / initialPassword / qualifiedBonus / goodBonus / excellentBonus / monthTaxableIncome` 等剩余英文字段。
- [前端] 首页“关于我们”标题不再拼接默认英文副标题 `ABOUT US`，避免员工页继续出现中英混排。
## 2026-04-06 V13.64（员工详情残留英文字段清理与考勤表自适应铺满）
### salary-system/src/main/resources/static/home.html
- [前端] 详情弹窗新增隐藏 `sourceType / statusTag` 两个内部派生字段，避免投诉反馈详情继续出现英文内部标识。
- [前端] 补齐 `monthSpecialDeduct / totalSocialSecurity / totalSpecialDeduct / accumulatedTaxable / recordDate / payDate / slipPublished / slipPublishTime` 的中文标签；`slipPublished` 改为“已发布/未发布”，相关时间继续统一去掉 `T`。
- [前端] 员工“考勤数据”表从固定 `width` 改为 `min-width` 自适应列宽，表格内容现在会横向铺满卡片区域，不再右侧留大块空白。
## 2026-04-06 V13.63（员工详情中文化、考勤页收口与异常上报过滤修复）
### salary-system/src/main/resources/static/home.html
- [前端] 为员工端详情弹窗补齐 `fullAttendBonus / otherIncome / grossSalary / reviewUserId / reviewComment / reviewTime / perfBonusRatio / managerId / managerName` 等字段的中文标签，并统一格式化 `status / applyType / reportType / feedbackType / calcStatus` 枚举值。
- [前端] 员工端详情与列表时间统一改为去掉 `T` 的中文展示格式；薪资反馈、考勤申请、投诉反馈单表不再直接显示 ISO 时间串。
- [前端] 员工“考勤数据”页表头调整为“月份 / 出勤天数 / 旷工天数 / 迟到天数 / 请假天数 / 加班小时”，并新增“打印考勤”按钮，支持员工自助打印当前列表。
### salary-system/src/main/java/com/salary/controller/AnomalyReportController.java
### salary-system/src/main/java/com/salary/service/AnomalyReportService.java
### salary-system/src/main/java/com/salary/service/impl/AnomalyReportServiceImpl.java
- [后端] 修正员工查看异常上报分页时的过滤条件：经理继续按 `reporter_id` 查看本人提交记录，员工改按真实 `employee.id -> emp_id` 过滤，修复员工投诉反馈单表误空白的问题。
## 2026-04-06 V13.62（管理端 token 真值校验与跨页面登录态迁移）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理员页初始化不再只依赖本地 `adminUserInfo`，而是启动后立即调用 `/api/auth/me` 校验 `adminToken` 对应的真实角色，避免浏览器残留缓存把页面角色看串。
- [前端] 若当前 `adminToken` 实际属于员工角色 `3`，会自动清理管理端存储、迁移到 `employeeToken/employeeUserInfo`，并跳转 `/api/home.html`，与员工页的反向拦截逻辑闭环一致。
- [前端] 对失效或非法 token 统一回收管理端登录态并返回登录页，降低“两个页面数据不同步但其实是串号”这一类假故障。
## 2026-04-06 V13.61（员工门户数据链路修复与投诉单表收口）
### salary-system/src/main/java/com/salary/controller/AttendanceController.java
### salary-system/src/main/java/com/salary/controller/AttendanceApplyController.java
### salary-system/src/main/java/com/salary/controller/SalaryController.java
### salary-system/src/main/java/com/salary/controller/TaxAccumulateController.java
### salary-system/src/main/java/com/salary/controller/PerformanceController.java
### salary-system/src/main/java/com/salary/controller/AnomalyReportController.java
- [后端] 员工自助查询与提交链路统一改为先由 JWT 中的 `userId` 反查员工档案 `employee.id`，修复考勤、考勤申请、薪资、个税、绩效、异常上报等模块因为主键口径不一致导致的员工页空数据问题。
- [后端] 员工提交考勤申请与异常上报时，入库的 `empId` / 过滤口径同步改回真实员工 ID，避免继续产生新脏数据。
### salary-system/src/main/resources/static/home.html
- [前端] 员工端考勤申请改为调用 `/api/attendance-apply/my`，投诉反馈改为调用员工自助接口并收口成单表；不再依赖未初始化完成的 `userInfo.id` 去拼管理员分页参数。
- [前端] 个税页与绩效页补齐字段映射：累计收入/累计社保/累计专项扣除/累计应纳税所得额/已缴个税改为读取真实累计字段，绩效总分与评语改为兼容 `score / evalComment`。
- [前端] 员工档案加载完成后会反写 `employeeUserInfo` 的 `id / empId / empNo / deptName / avatar`，降低后续表单与明细页再次取数时的上下文丢失风险。
## 2026-04-06 V13.60（管理端与员工端双登录态隔离）
### salary-system/src/main/resources/static/login.html
- [前端] 登录成功后不再写入共享 `token/userInfo`，而是按真实角色分别写入 `adminToken/adminUserInfo` 或 `employeeToken/employeeUserInfo`；部门经理仍归入管理后台登录态。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理后台改为只读取 `adminToken/adminUserInfo`，请求拦截器、页面初始化、401 失效处理、退出登录都只清理管理端登录态；若仅存在员工登录态则自动回跳员工门户。
### salary-system/src/main/resources/static/home.html
- [前端] 员工门户改为只读取 `employeeToken/employeeUserInfo`，请求拦截器、页面初始化、401 失效处理、退出登录都只清理员工登录态；若仅存在管理端登录态则自动回跳管理后台。
- [兼容] 新增旧版共享 `token/userInfo` 到双登录态存储桶的迁移逻辑，降低版本切换时的本地缓存冲突风险。
## 2026-04-06 V13.59（员工门户角色门禁收紧与自动回跳）
### salary-system/src/main/resources/static/home.html
- [前端] 为员工门户补齐与管理员页一致的角色归一化逻辑，统一将本地 `userInfo.role` 转为数值后再做页面门禁判断。
- [前端] 收紧 `/api/home.html` 进入条件：仅 `role=3` 的员工可留在员工界面；`role=1/2` 的管理员与部门经理在已登录状态下会自动跳回 `/api/admin/index.html`。
- [前端] 对无效角色或缺失登录态继续清空 `localStorage` 并返回 `/api/login.html`，避免员工页与管理页共用同一账号视图。
## 2026-04-06 V13.58（管理员薪资发放逐行上传发放文件）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 将管理员“薪资发放”页的“发放文件”列改为每行单独上传：无文件时显示“上传文件”，有文件时显示文件链接与“重新上传”，并接入上传中状态与上传后列表刷新。
### salary-system/src/main/java/com/salary/entity/SalaryRecord.java
### salary-system/src/main/java/com/salary/service/SalaryService.java
### salary-system/src/main/java/com/salary/service/impl/SalaryServiceImpl.java
### salary-system/src/main/java/com/salary/controller/SalaryController.java
- [后端] 为 `SalaryRecord` 补齐 `issueFile` 字段、新增 `updateIssueFile` 保存链路，并提供 `POST /salary/{id}/issue-file` 上传接口，仅允许管理员上传指定格式的发放附件。
### db_schema.sql / 运行库 bishe.t_salary_record
- [数据库] 为 `t_salary_record` 新增 `issue_file` 字段并完成运行库同步，确保发放文件可随薪资记录持久化并回显到管理员发放列表。
## 2026-04-06 V13.57（管理员表格勾选删除与投诉页单表收口）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 为管理员端部门、部门经理、考勤数据、考勤申请、异常上报、投诉反馈、薪资反馈、个税社保、绩效考核、薪资核算、薪资发放等业务表格补齐勾选列，并在表格上方统一增加删除按钮。
- [前端] 为有后端删除接口的模块补齐批量删除逻辑；对薪资反馈、个税社保、薪资核算、薪资发放等暂无删除接口的模块改为明确提示，避免误导为已删除。
- [前端] 将“部门经理”页操作栏对齐员工管理页，统一成“新增 / 删除 / 导入 / 导出”四按钮，并把“投诉反馈”从原双表结构收口为单表，保留详情、处理与分页能力。

## 2026-04-06 V13.56（登录角色分流与管理员页门禁修复）
### salary-system/src/main/resources/static/login.html
- [前端] 登录成功后将 `role` 统一转为数值再写入 `localStorage`，并按数值角色分流：`1/2` 进入管理员端，`3` 进入员工门户。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 新增管理员页角色归一化与进入门禁：若当前登录态是员工角色，则自动跳回 `../home.html`；若角色非法，则清空登录态并返回 `../login.html`。
- [前端] 由于管理员页后续权限判断大量使用严格等于，角色归一化后同步修复了“右上角显示成员工、操作列消失”的联动问题。
### salary-system/src/main/resources/static/home.html
- [前端] 定点收口员工详情弹窗，隐藏 `salaryRecordId / replyUserId / reporterId / processorId` 等内部 ID 字段。
- [前端] 补齐 `applyType / replyUserName / replyTime / processTime / description / reporterName / processorName` 的中文标签映射，避免详情弹窗直接显示英文字段名。

## 2026-04-06 V13.55（员工门户分页样式对齐管理员端）
### salary-system/src/main/resources/static/home.html
- [前端] 复用管理员端分页皮肤，为员工门户新增 `.page-footer` 分页容器及同款圆角、边框、激活态渐变、高亮阴影样式。
- [前端] 将员工端 8 处 `<el-pagination>` 统一改为管理员端同款参数，去掉旧 `background` 外观，并把 `page-sizes` 对齐为 `[10, 20, 50, 100]`。
- [前端] 补齐员工端分页的 `v-model:page-size` 与 `@size-change` 绑定，保证切换每页条数后的刷新行为一致。
## 2026-04-06 V13.54（员工门户替换字符定点修复）
### salary-system/src/main/resources/static/home.html
- [前端] 仅修复 `home.html` 内 3 处实际出现的 `U+FFFD` 替换字符，未重写整页结构或脚本逻辑。
- [前端] 按当前页面上下文恢复菜单文案 “工资个税与社保”、空态提示 “暂无最新公告”、个人信息弹窗提示尾句 “联系人事”。
- [编码] 保持本次修改以 UTF-8 写回，避免继续引入新的替换字符。
## 2026-04-06 V13.53（员工门户弹窗全量统一设计语言）
### salary-system/src/main/resources/static/home.html
- [前端] 引入由 `modern-dialog` 与 `dialog-header-gradient` 构成的全套现代 UI 弹窗主题。
- [前端] 完善个人信息弹层组件重塑，分设上下板块（固化只读区域 + 可修改区域，新增支持实名/性别/二代证等修改项）。
- [前端] 全面升级覆盖 `修改密码`、`考勤事件申请`、`异常上报专栏`、`投诉反馈` 及 `薪资反馈工单` 这 5 个交互频次最高的弹窗表单至 modern-dialog 风格！
- [前端] **分页控制**：为考勤数据、考勤申请、薪水单、薪资反馈、异常上报、投诉反馈等 **6大表格** 全部接入 Element Plus 底层 `<el-pagination>`。
- [前端] JS 端重构 `loadMyData()`，拆分为 `loadAttendances()` 等细分加载器，封装统一的 `tablePaging` 响应式状态进行单页容量限制（size=10）与总条目防溢出控制。

## 2026-04-06 V13.52（员工门户界面修复第一轮）
### salary-system/src/main/resources/static/home.html
- [前端] 顶栏右侧增加"首页/通知公告"快捷链接，头像从写死 `front_assets` 改为动态绑定 `empProfile.avatar || userInfo.avatar`。
- [前端] 导航菜单对齐为：个人中心/修改密码/考勤申请/考勤数据/薪资核算/薪资反馈/工资个税与社保/绩效评分/投诉反馈（共9项），菜单宽度从1000px扩至1200px。
- [前端] 个人中心排版重构：从左右分栏改为上下结构——上方基本信息全宽卡片（3列 el-descriptions，右上角带"修改个人信息"按钮触发弹窗），修改密码拆为独立面板。
- [前端] 新增"修改个人信息"弹窗（el-dialog），编辑手机号和银行卡号。
- [前端] 修复 saveEmpProfile 保存失败 Bug：主键从 `empProfile.id` 改为 `empProfile.empId || empProfile.id`，修复因后端 `/api/auth/me` 返回字段名不一致导致的请求体主键丢失。
- [前端] empProfile reactive 对象增加 `empId` 初始字段，确保 Vue3 响应式追踪。

### salary-system/src/main/java/com/salary/service/impl/UserServiceImpl.java
- [后端] `getCurrentUserInfo` 接口补全：将 role==2（经理）也纳入员工档案补全范围（原来只有 role==3 才走 Employee 表）。
- [后端] 返回字段从原来的 6 个（empNo/empId/deptName/phone/hireDate/bankAccount）扩展为 17 个，新增 id/deptId/bankCard/gender/idCard/baseSalary/positionName/position/managerNo/managerName/status/avatar。
- [后端] 头像优先用员工档案表的 avatar，缺失时兜底用 t_user 表的 avatar。


## 2026-04-05 V13.51（经理/员工测试账号登录修复）
### 数据库 `t_user`
- [数据] 排查确认登录问题根因是测试账号种子密码与当前默认口令不一致，而不是前端角色选择、验证码或后端统一登录接口 `/api/auth/login` 出错。
- [数据] 将角色为 `2/3` 的经理、员工测试账号密码统一重置为 bcrypt(`123456`)，管理员账号保持现状不变；修复后可使用 `10001~10005 / 001~010 + 123456` 登录。

### DEV_PLAN.md
- [归档] 新增并完成“经理/员工测试账号登录修复”任务。

## 2026-04-05 V13.50（项目级安装论文技能 lunwen-skill）
### E:\bishe\.agents\skills\lunwen-skill
- [技能] 使用 GitHub 仓库 `Doryoku1223/lunwen-skill` 安装项目级论文技能到 `E:\bishe\.agents\skills\lunwen-skill`，遵循 `.cursorrules / AGENTS.md` 约定，仅写入项目目录，不写入全局 `C:\Users\614377781\.codex\skills`。
- [技能] 核对仓库主入口为 `SKILL.md`，当前 Codex 可按项目级技能目录读取；安装结果同时包含 `agents/`、`prompts/`、`references/`、`tools/` 等完整工作流资源。
- [兼容性] 核对仓库自带 `.claude/` 与 `.trae/` 兼容层，适合 Claude / Trae 类宿主复用；当前仓库未内置 `.cursor/` 专用入口，因此 Cursor 若要原生复用同一套技能封装，仍需单独适配。

## 2026-04-05 V13.49（薪资反馈记录中心与角色范围收口）
### salary-system/src/main/java/com/salary/controller/SalaryFeedbackController.java
- [后端] 新增 `GET /feedback/salary-page` 记录中心接口：管理员查看全部薪资反馈，经理查看本人及下属员工反馈，员工仅查看本人反馈，独立于投诉/异常反馈列表。

### salary-system/src/main/java/com/salary/service/SalaryFeedbackService.java
### salary-system/src/main/java/com/salary/service/impl/SalaryFeedbackServiceImpl.java
- [后端] 为薪资反馈分页增加“多 empId 范围查询”能力，支持经理基于“本人 + 下属员工”集合拉取薪资反馈记录。

### salary-system/src/main/resources/static/home.html
- [前端] 新增独立“薪资反馈”菜单与记录页，工资条操作列继续保留“薪资反馈”作为提交入口；记录页直接展示账期、反馈类型、反馈标题、反馈内容、回复内容、处理状态与提交时间。
- [前端] 原“投诉反馈与异常上报”页面继续只承载异常上报和投诉反馈（类型 3），不再混入薪资异议记录，避免员工/经理在同一页看到两套不同业务。

### DEV_PLAN.md
- [归档] 新增并完成“薪资反馈记录中心与角色范围收口”任务。

## 2026-04-05 V13.48（税社保详情与支付面板排版放松）
### admin/index.html
- [前端] 放大“工资个税与社保”详情弹窗的标题、副标题、标签和值字号，并提升卡片内边距与行高，缓解信息块过于拥挤的问题。
- [前端] 重新整理“薪资发放”支付面板的摘要条、渠道卡和当前渠道提示区域，调整为更宽松的图标与文字排版，避免真实图标接入后内容过密。

### DEV_PLAN.md
- [归档] 新增并完成“税社保详情与支付面板排版放松”任务。

## 2026-04-05 V13.47（投诉反馈页双区块结构收口）
### admin/index.html
- [前端] 为“异常上报 / 投诉反馈”共享页面补上分区标题：上半区固定为“异常上报记录”，下半区维持“投诉反馈记录”，避免当前页在双列表并存时缺少结构锚点。
- [前端] 投诉反馈页正式收口为“两段式记录页”结构，保留下半区的投诉反馈列表样式作为后续该模块的统一基线。

### DEV_PLAN.md
- [归档] 将第 25 项子任务“投诉反馈页双区块结构基线”标记完成。

## 2026-04-05 V13.46（绩效奖金口径统一与测试数据清理）
### salary-system/reset_data.sql
- [数据] 将 `t_performance` 的种子数据从旧的比例值口径（`1.00 / 1.10 / 1.20`）统一改为当前系统使用的绝对奖金金额口径（`1200 / 1500`），并把旧的 `A / B / S` 等级文案切回当前页面与后端统一使用的中文等级。

### 运行库（bishe.t_performance）
- [数据] 直接清理当前运行库中的旧测试绩效口径：按现有评分阈值统一把 `grade` 和 `perf_bonus_ratio` 刷成当前规则下的中文等级与绝对奖金金额，避免后续继续出现“字段已修复但库里仍是旧比例值”的假故障。

### DEV_PLAN.md
- [归档] 将第 25 项子任务“绩效评分保存超范围 / 奖金口径不一致”标记完成，当前后端字段精度、运行库测试数据与重置种子文件已统一到同一套绩效奖金规则。

## 2026-04-05 V13.45（支付面板图标与排版精修）
### admin/index.html
- [前端] 为薪资发放支付面板接入本地真实渠道图标：`微信 / 支付宝 / 建设银行 / 中国银行`，替换原先的纯文字字块占位。
- [前端] 将支付弹窗宽度、渠道卡留白、品牌字号与说明行距重新整理，缓解文字竖排和内容拥挤问题，使整体更接近用户给出的支付参考图。
- [前端] 将支付图标路径改为 `./img/payment/...` 相对静态资源引用，避免管理端从 `admin/index.html` 进入时出现资源路径不稳定。

### DEV_PLAN.md
- [归档] 新增并完成“薪资发放支付面板图标与排版精修”任务。

## 2026-04-05 V13.44（薪资发放支付面板改为参考图样式）
### admin/index.html
- [前端] 将“企业薪资统一发放网关”改造成更接近用户第二张参考图的横向支付面板风格，保留弹窗承载方式，但把纵向大卡片交互改为“核对摘要 + 横向渠道条 + 当前渠道提示”的收银台布局。
- [前端] 支付渠道文案全部改成真实名称：`微信 / 支付宝 / 建设银行 / 中国银行`，移除“企业版 / 商户代发 / 公户直连”等抽象命名。
- [前端] 将支付主按钮文案改为“确认支付”，辅按钮改为“返回”，并同步收口支付中与支付成功提示语，保持与参考图一致的流程语义。

### DEV_PLAN.md
- [归档] 第 30 项后两条“支付页目标样式 / 支付确认反馈”已完成并勾选。

## 2026-04-05 V13.43（社保比例与累计个税口径一致性收口）
### DEV_PLAN.md
- [计划] 将第 30 项补充为用户确认后的最终口径：个税继续采用国家要求的累计预扣预缴法，仅忽略专项附加扣除；支付页目标样式允许做成弹窗或单独页面，且渠道名称必须使用 `支付宝 / 微信 / 建设银行 / 中国银行` 这类真实名称。

### salary-system/src/main/java/com/salary/entity/SocialSecurityConfig.java
- [后端] 同步修正社保配置实体注释，将个人失业保险比例更新为 `0.3%`，将住房公积金个人比例更新为 `12%`，并明确单位缴纳字段当前仅作展示备用，不参与个人税前扣除。

### salary-system/src/main/java/com/salary/entity/TaxAccumulate.java
- [后端] 调整累计个税实体说明，明确本系统仍按累计预扣预缴法计算个税，但专项附加扣除字段当前固定按 `0` 处理；避免后续维护时误以为系统已启用专项附加扣除。

### salary-system/src/main/java/com/salary/entity/SalaryRecord.java
- [后端] 修正薪资记录实体中个人五险一金扣款比例注释，统一为 `养老8% + 医疗2% + 失业0.3% + 公积金12%` 当前口径。

### db_schema.sql
- [数据库设计] 更新 `t_tax_accumulate` 相关字段注释，将应纳税所得额描述改为“应发工资 - 个人五险一金 - 5000起征点”，并声明专项附加扣除字段当前按 `0` 处理。
- [数据库设计] 更新 `t_social_security_config` 默认值与注释，将个人失业保险比例默认值从 `0.0050` 改为 `0.0030`，将个人公积金比例默认值从 `0.0500` 改为 `0.1200`。

### salary-system/reset_data.sql
- [种子数据] 更新重置脚本中的社保配置默认值与通知公告文案，失业保险个人比例改为 `0.3%`、住房公积金个人比例改为 `12%`。
- [种子数据] 更新薪资反馈示例中的个税说明，明确当前系统仅忽略专项附加扣除，不改变累计预扣预缴法主体逻辑。

## 2026-04-05 V13.42（工资个税与社保经理字段强制对齐部门经理表）
### admin/index.html
- [前端] 调整 `taxDisplayRows` 的人物解析顺序，税社保页当前员工仅按 `empId / empNo` 匹配本人资料，不再把税社保记录中的 `managerNo` 传入员工头像回查，彻底切断员工头像与经理字段的串值链路。
- [前端] 经理信息改为“部门经理资料优先、税社保原始 `managerNo/managerName` 仅作兜底”，强制以部门配置的真实经理为准，修复该页前几行显示成 `001 王建国 / 003 陈志远 / 005 孙浩然` 等错误经理的问题。
- [前端] 同步补齐 `deptName / deptId` 的派生口径，确保税社保表格和详情页的部门、经理信息继续和“部门经理”模块保持一致。

## 2026-04-05 V13.41（工资个税与社保员工头像/部门经理映射纠偏）
### admin/index.html
- [前端] 修正 `resolveEmployeeProfile`，员工资料回查只允许按当前行 `empId / empNo` 命中，不再使用 `managerNo` 反向匹配，避免部门经理行被误识别成下属员工或首字母占位头像。
- [前端] 修正 `resolveManagerProfile`，部门经理统一优先按“部门配置的真实 `managerId/manager_id` -> 经理资料”回查，仅在缺失时再按经理工号回退，杜绝“工资个税与社保”页把部门经理显示成普通员工。
- [前端] 收口税社保列表中的经理姓名/账号派生逻辑，移除对 `employee.managerName / employee.managerNo` 的错误兜底，确保该页经理信息与“部门经理”模块保持一致。

## 2026-04-05 V13.40（薪资发放支付弹窗样式升级）
### admin/index.html
- [前端] 将“企业薪资统一发放网关”从旧的即时点击支付块升级为卡片式渠道选择交互，提供“银行公户直连 / 支付宝企业版 / 微信商户代发”三种更接近真实收银台的视觉样式。
- [前端] 新增 `selectedPaymentChannel` 状态，将发放流程改为“先选渠道，再确认发放”，避免误触即发放；未选择渠道时禁用主按钮并给出提示。
- [前端] 收口支付弹窗中的摘要、渠道提示与成功态样式，复用统一的 `payment-channel-*` 设计类，保持与全站现代卡片风格一致。

## 2026-04-02 V13.39（工资个税与社保部门经理映射修正）
### admin/index.html
- [前端] 重写 `resolveEmployeeProfile` 的工号匹配顺序，禁止再用普通员工记录上的 `managerNo` 反向命中当前员工，修复部门经理行在“工资个税与社保”页误匹配为下属员工的问题。
- [前端] 调整 `resolveManagerProfile` 的回查链路，优先按 `部门 -> managerId -> 经理资料` 定位部门经理，仅在缺失时再按经理工号回退，确保“经理账号 / 部门经理”与“部门经理”模块保持一致。

## 2026-04-02 V13.38（工资个税与社保头像链路修正）
### admin/index.html
- [前端] 修正“工资个税与社保”左侧员工头像来源，改为统一走 `resolveEmployeeProfile(empId, empNo, managerNo)` 回查，解决前 5 条部门经理记录误退回灰色首字母头像的问题。
- [前端] 撤回误加的“经理头像”列，保留经理账号和部门经理文本列，避免列表中同时出现员工头像与重复的经理头像造成视觉混乱。
- [前端] 税社保详情页同步移除“经理头像”卡片，仅保留员工头像主视觉和经理文本信息，页面信息层级与用户要求保持一致。

## 2026-04-02 V13.37（工资个税与社保经理展示与薪资基数补齐）
### admin/index.html
- [前端] 在“工资个税与社保”列表中新增“薪资基数”列，并补充“经理头像”列，统一复用 `el-image + 预览` 交互，避免列表只能看到经理账号/姓名却缺少人物识别。
- [前端] 为税社保详情弹窗补充“薪资基数”和“经理头像”展示，保持详情页与表格主数据口径一致，不再只展示员工头像。
- [前端] 新增 `resolveManagerProfile` 经理资料回查函数，将 `deptManagers / departments / 本地头像缓存` 收口到同一条解析链路，统一解决经理姓名、账号、头像在列表和详情里的缺失问题。
- [前端] 同步修正“工资个税与社保”导出字段，补充 `薪资基数 / 失业保险`，并将导出中的日期字段改为真实的 `deductDate`。

## 2026-04-02 V13.36（薪资发放审核状态可视化补齐）
### admin/index.html
- [前端] 将“薪资发放”搜索栏中的“支付状态”收口为更准确的“账单状态”，支持直接筛选 `待审核 / 已审核 / 已发放`。
- [前端] 在“薪资发放”列表中新增“审核状态”列，明确区分“待审核不可发放”与“已审核可发放”，并与现有“是否支付 / 发布状态”分层展示。
- [前端] 延续此前修复的“发放前不显示发放日期”口径，发放日期仅在真实支付完成后展示。

## 2026-04-02 V13.35（薪资反馈列表补回复内容列）
### admin/index.html
- [前端] 在管理员端“薪资反馈 / 投诉反馈”共用列表模板中，于“反馈内容”后新增“回复内容”列，直接展示 `replyContent`，未回复时显示 `-`。
- [前端] 保留现有详情弹窗链路不变，管理员现在无需再点进详情，即可在列表层直接查看处理回复结果。

## 2026-04-02 V13.34（清空旧绩效测试数据并重算 2026-04 薪资）
### 运行中数据库 / SalaryServiceImpl.java（复用现有算薪逻辑）
- [数据库] 根据用户确认“当前均为测试数据，可直接重置”的口径，清空了旧的 `t_performance` 与 `t_tax_accumulate` 测试数据，不再对旧比例值做历史映射。
- [数据库] 使用一次性维护入口重建了 `2026-04` 月的绩效测试数据，并复用现有 `SalaryService.calculateSalary / batchCalculate` 逻辑将 `2026-04` 薪资全部按新绩效规则重算。
- [数据库] 重算后，`t_salary_record.perf_bonus` 已从旧的高额快照值（如 3000 / 2400 / 2200）回落到当前绩效评分规则下的奖金金额口径（1500 / 1200 / 900 / 0）。
- [数据库] 重算过程中保留了 `calc_status / pay_date / slip_published / slip_publish_time / record_date`，避免影响现有审核、发放和工资条发布状态。

### 仓库清洁
- [维护] 这次用于数据重置的一次性维护入口和失败的临时测试文件已在执行完成后立即删除，仓库未保留临时脚本或测试垃圾文件。

## 2026-04-02 V13.33（绩效奖金联动精度、个税列与薪资发放头像修复）
### Performance.java / db_schema.sql / salary-system/reset_data.sql / 运行中数据库
- [后端/数据库] 将 `t_performance.perf_bonus_ratio` 的表结构口径从旧的“绩效系数”切换为可落库真实奖金金额的 `DECIMAL(10,2)`，并同步更新实体注释与建表脚本，修复绩效评分保存 900 / 1200 / 1500 等绝对奖金金额时的字段越界问题。
- [数据库] 已对运行库执行字段精度升级，仅修改列定义，不回写历史绩效数据，避免把旧种子数据误转换为错误金额。

### admin/index.html
- [前端] 为管理员端“薪资核算”与“薪资发放”表格补充独立的“个税”列，并同步把导出列集补齐为 `绩效奖金 / 津贴 / 个税 / 扣款金额 / 实发工资`，避免界面只显示总扣款而无法区分个税。
- [前端] 将“薪资发放”页面的人物资料回查切换为统一的 `resolveEmployeeProfile(empId, empNo, managerNo)` 链路，修复部门经理行记录只显示首字母圆头像、无法命中真实头像的问题。
- [前端] 同步补齐 `salaryDisplayRows / paymentDisplayRows` 的 `incomeTax` 映射，保持前端列表、导出和详情口径一致。

## 2026-04-02 V13.32（薪资审核流与工资条发布门控收口）
### SalaryController.java / SalaryService.java / SalaryServiceImpl.java / SalaryRecord.java / SalaryRecordMapper.java / SalaryRecordMapper.xml
- [后端] 薪资分页查询补齐角色分流参数：经理端继续看“草稿 / 待审核 / 已审核 / 已发放”，且自动排除经理本人；管理员端默认排除草稿，只保留待审核后的账单视角。
- [后端] 为 `SalaryRecord` 新增 `slipPublished / slipPublishTime` 字段，并在薪资支付后默认重置为“未发布”，将“已支付”和“工资条已开放查看”拆成两条独立状态链路。
- [后端] 新增 `publishSalarySlip / publishSalarySlips` 服务与 `/salary/{id}/publish-slip`、`/salary/batch-publish-slip` 接口，要求管理员在薪资发放页显式发布工资条后，员工 / 经理端才可查看该月工资。
- [后端] 员工侧 `getMyHistory / getByEmpAndMonth` 增加工资条可见性门控，仅返回 `calc_status=4` 且 `slip_published=1` 的记录，堵住前端绕过按钮直接查看未发布工资条的口子。
- [后端] 工资条发布时自动补发一条“YYYY-MM 工资条已发布”的系统公告，避免员工端收到工资条开放后没有通知提示。

### admin/index.html / home.html
- [前端] 管理员薪资核算页移除“提交审核”和草稿筛选，只保留 `批量审核 / 导出` 主操作；经理端继续保留 `核算 / 提交审核 / 导出`，状态筛选完整显示草稿到已发放。
- [前端] 将薪资核算状态文案统一为 `草稿 / 待审核 / 已审核 / 已发放`，彻底去掉“已发布(待审)”这种与薪资发放页混淆的旧表述。
- [前端] 薪资发放页新增“批量发布工资条”按钮、行内“发布”动作和“发布状态”列，支付后必须再发布，员工端才可查看；发放日期也改成仅在真实支付后显示，不再回退到登记日期。
- [前端] 员工 / 经理门户 `home.html` 的“我的薪水”改为调用 `/api/salary/my/history`，不再直接使用 `/api/salary/page`；经理门户也纳入工资条门控范围。

### db_schema.sql / salary-system/reset_data.sql / 运行中数据库
- [数据库] 为 `t_salary_record` 表结构补充 `slip_published`、`slip_publish_time` 字段。
- [数据库] 已对运行库执行安全回填：仅将“当前月之前且已发放”的历史工资记录标记为已发布，避免污染本月尚未发布的工资条。

## 2026-04-02 V13.31（薪资核算审核流交互语义收口）
### admin/index.html
- [前端] 为薪资核算搜索栏新增“状态筛选”，支持按草稿 / 待审核 / 已审核 / 已发放过滤，方便管理员先筛出待审核账单再手动勾选批量审核。
- [前端] 将薪资核算页顶部“发布”按钮改为“提交审核”，保留原有 `batchPublishSalary` 提交流程，但在界面语义上明确其职责是把草稿送入待审核队列，而不是发放工资条。
- [前端] 将“批量审核”提升为薪资核算页主操作按钮，并同步收口相关提示文案，将“已发布(待审)”统一表达为更直白的“待审核”，避免与薪资发放页的真实发放动作混淆。

## 2026-04-02 V13.30（薪资核算审核文案与经理基础工资显示收口）
### admin/index.html
- [前端] 将薪资核算顶部第 3 个按钮由“二审”改为“批量审核”，单笔操作改为“单笔审核”，并同步收口审核确认弹窗、成功提示与门控文案，避免“二审”语义过于技术化。
- [前端] 将薪资核算列表的人物资料解析切换为统一的 `resolveEmployeeProfile(empId, empNo, managerNo)` 回查链路，确保部门经理记录在前端显示基础工资时也按“部门基础工资 + 岗位工资”识别，不再因只查员工表导致经理口径漂移。
- [前端] 同步修正薪资详情与薪资反馈弹窗的头像来源，统一按“员工表 / 经理表 / 本地头像缓存”回溯，避免列表修正后详情仍走旧分支。

## 2026-04-02 V13.29（考勤筛选与申请列表信息补齐）
### admin/index.html
- [前端] 为“考勤数据”搜索栏新增月份筛选，并将该筛选参数接入 `/api/attendance/page` 请求；保留操作区“导入月份”仅作为导入用途，避免筛选与导入语义混淆。
- [前端] 为“录入考勤数据”弹窗补齐“员工 / 经理”统一下拉数据源，支持手动录入时选择部门经理，不再只限普通员工。
- [前端] 为“考勤申请”列表新增月份筛选与“申请理由”列，并将申请时间统一格式化为无 `T` 的展示文本，同时保留“审核回复”直接列表可见。

## 2026-04-02 V13.28（考勤数据头像来源统一）
### admin/index.html
- [前端] 为考勤数据列表与考勤详情补齐统一的人物资料解析函数，按“员工表 + 经理表 + 本地头像缓存”三层顺序回查头像与基础资料，修复经理账号记录只显示灰色首字母头像的问题，并保持列表与详情取数口径一致。

## 2026-04-02 V13.27（考勤申请重复头像列修复）
### admin/index.html
- [前端] 删除“考勤申请”列表中误插入的第二个头像列，恢复为单一员工头像列展示，避免表头重复和列表视觉错位。

## 2026-04-02 V13.26（基础工资显示联动与人像补齐）
### admin/index.html
- [前端] 将薪资核算、工资个税与薪资发放页中的基础工资显示口径切换为优先读取最新部门/员工档案联动结果，解决部门基础工资重新保存后列表仍显示旧值的问题；该调整只影响页面展示，不回写历史薪资快照。
- [前端] 为考勤数据、绩效评分、薪资核算、薪资发放等和人相关的表格补齐头像列，并统一接入 el-image 预览交互。
- [前端] 修复考勤详情、绩效详情、薪资详情、工资个税详情的头像来源回溯，优先读取行数据头像，再回查员工档案与本地头像缓存，避免详情页只剩首字母占位。
## 2026-04-02 V13.25（部门基础工资事件联动与反馈入口分流）
### DepartmentServiceImpl.java / SalaryFeedbackServiceImpl.java
- [后端] 重写 DepartmentServiceImpl，修复坏字符串与编译问题，并固定“部门变更 -> 批量回写员工档案 baseSalary”的事件触发式联动；仅更新员工基础档案，不修改历史 SalaryRecord / 社保快照。
- [后端] 保留部门经理工资口径：部门经理基础档案工资 = 部门基础工资 + 职位工资；普通员工 = 部门基础工资。
- [后端] 重写 SalaryFeedbackServiceImpl，修复反馈处理服务中的坏字符串和分页过滤链路，保证 
eedbackType 分流后的反馈查询与回复逻辑可正常编译执行。
### admin/index.html / home.html
- [前端] 管理端移除单独的“异常上报”导航入口，投诉反馈页面继续合并展示异常处理与投诉反馈，薪资反馈保持独立页面。
- [前端] 员工/经理端将薪资反馈按钮从考勤表移至薪资记录操作列；“投诉反馈”页面保留投诉/异常入口，不再混用薪资异议入口。
- [前端] 员工端投诉反馈弹窗改为投诉语义文案，和薪资反馈弹窗分离，形成两套独立入口体验。
## 2026-04-02 V13.24（考勤申请审批弹窗 DOM 解析修复）
### admin/index.html
- [前端] 将考勤申请审批弹窗头部误用的 `<Select />` 图标组件替换为 `CircleCheckFilled`。由于当前后台采用原生 HTML 模板挂载 Vue，`<Select />` 会被浏览器当成原生 `<select>` 解析，直接破坏后续弹窗 DOM，导致“同意/驳回”等按钮点击后出现空白小框或无响应。
- [前端] 修复后续共享 `modern-dialog / detail-dialog` 链路的模板解析稳定性，优先恢复“考勤申请之后”一组操作弹窗的正常打开行为。

## 2026-04-02 V13.23（后续详情窗金额乱码与按钮文案统一）

### admin/index.html
- [前端] 继续清理“考勤申请之后”相关详情窗中的金额前缀乱码，将员工详情、薪资发放网关、工资个税与社保详情中的异常 `楼` 字统一恢复为 `￥`。
- [前端] 去掉个税调整弹窗中不统一的 emoji 按钮文案，统一为纯中文操作文案，保持整套 `modern-dialog / detail-dialog` 风格一致。

## 2026-04-02 V13.22（公告详情页返回按钮强化与占位区修复）

### admin/index.html
- [前端] 将公告详情页返回按钮升级为统一风格的高辨识度胶囊按钮，增加左箭头图标、主色描边和更明显的视觉层级，避免右上角弱按钮不易发现。
- [前端] 为公告详情页补充 `notice-detail-shell / notice-detail-back` 等样式类，统一与现有现代化详情卡片风格。
- [前端] 修复 `notice-detail` 未加入页面白名单的问题，彻底移除底部“功能页面开发中...”占位块误显示。

## 2026-04-02 V13.21（公告同步、独立公告详情页与后续操作弹窗收口）

### AnnouncementController.java / AnnouncementService.java / AnnouncementServiceImpl.java
- [后端] 扩展 `GET /announcement/list`，新增可选 `status` 过滤，并统一公告排序为 `isTop DESC, pubTime DESC, createTime DESC`，保证首页“最新通知公告”和“通知公告”列表顺序一致。
- [后端] 新增 `GET /announcement/{id}` 公告详情接口，用于前端独立详情页加载。
- [后端] 调整公告新增 / 修改默认值：未显式传入时自动补齐 `status=1`、`isTop=0`，并兜底 `pubTime`，避免新公告因状态为空而无法在首页与列表同步显示。

### admin/index.html
- [前端] 新增独立 `notice-detail` 页面状态和 `noticeDetail / noticeDetailSource / loadNoticeDetail / backFromNoticeDetail` 详情链路；首页点击公告和通知公告列表“详情”按钮现在都会进入同一个公告详情页，而不是再走旧弹窗或仅跳列表。
- [前端] 首页“最新通知公告”和通知公告列表统一改为请求已发布公告数据；公告新增、修改、删除后同时刷新首页公告区与通知公告列表，修复两处内容不同步的问题。
- [前端] 为异常上报、投诉反馈表格补齐“详情”按钮，并将对应详情统一接入共享 `detail-dialog` 模板。
- [前端] 清理投诉反馈区损坏的表格列结构，避免浏览器容错破坏后续 DOM，连带修复“考勤申请之后”部分操作按钮无响应或弹出异常小框的问题。
- [前端] 将审批 / 异常 / 反馈 / 绩效弹窗的 Hero eyebrow 文案统一切回中文，继续收口后续模块的视觉风格。

## 2026-04-02 V13.20（修复启动报错 ClassNotFoundException）

### EmployeeServiceImpl.java
- [后端] 修复 `ClassNotFoundException: Department`。在 `EmployeeServiceImpl.java` 中补充了缺失的 `Department` 实体类导入，解决由于反射解析失败导致的 Spring Boot 启动挂起问题。

## 2026-04-02 V13.19（首页公告跳转与图表空月份归零）

### admin/index.html
- [前端] 将首页“最新通知公告”从详情弹窗改为点击直接进入“通知公告”模块，并自动带入当前公告标题执行查询，符合首页作为入口页的使用习惯。
- [前端] 为首页“当月整体考勤状态 / 各部门人均薪资”两个月份选择器增加直接 `change` 刷新链路，切月时不再只依赖 `watch`。
- [前端] 调整这两张图的重绘逻辑：切月时先清空旧图；若所选月份无数据，则按 0 渲染，避免继续显示上一个月份的残留统计。

## 2026-04-02 V13.18（考勤扣款规则收紧与首页公告/统计修复）

### AttendanceServiceImpl.java / AttendanceRecordMapper.xml
- [后端] 将考勤扣款规则正式落地到服务层：迟到/早退按次扣款，旷工按日薪全额扣款，事假按日薪全额、病假按日薪 50% 扣款；若数据库存在启用中的 `AttendanceRule` 则优先按配置计算。
- [后端] 新增、修改、Excel 导入考勤记录时自动回填 `attendDeduct`，修复考勤记录长期只存 0、薪资核算只能临时兜底的问题。
- [后端] 修正首页考勤状态统计 SQL 中残留未转义的 `year_month` 字段，并调整“全勤正常”统计口径，避免首页图表接口异常后被前端兜成 0。

### admin/index.html
- [前端] 修复首页“最新通知公告”列表点击无效问题，首页公告项现在可直接打开统一详情弹窗。
- [前端] 统一首页公告发布时间格式，去掉 ISO 时间中的 `T`，并新增单行时间样式，避免日期被挤压折断。
- [前端] 为首页考勤状态图和部门人均薪资图加入空数据文案，并补充月份 `watch` 监听，保证月份切换时图表一定刷新，不再出现选了月份但图没变的假象。

## 2026-04-02 V13.17（首页图表独立月份选择器拆分）

### admin/index.html
- [前端] 重构首页图表卡片头部，为“当月薪资结构占比 / 月实发工资走势 / 当月整体考勤状态 / 各部门人均薪资”分别加入独立月份选择器，互不干扰。
- [前端] 将首页图表加载逻辑从单一 `loadDashboard -> Promise.all -> initCharts` 拆分为单图表独立请求与独立渲染，切换某个图表月份时只刷新当前图表。
- [前端] 新增首页图表头部样式 `dashboard-card-header / dashboard-card-picker`，保证标题与月份控件共存时仍保持整洁布局。
- [前端] 补齐首页“最新通知公告”独立加载逻辑，避免首页只刷新图表而公告区为空。

### SalaryController.java / SalaryService.java / SalaryServiceImpl.java / SalaryRecordMapper.java / SalaryRecordMapper.xml
- [后端] 为 `GET /salary/stat/trend` 补充 `yearMonth` 参数，趋势图按所选月份为右边界返回最近 12 个月数据。
- [后端] 为 `GET /salary/stat/dept-avg` 补充 `yearMonth` 参数，使“各部门人均薪资”支持指定月份统计而不是全量平均。

## 2026-04-02 V13.16（薪资核算与考勤 / 绩效 / 部门联动修复）

### SalaryServiceImpl.java
- [后端] 重写薪资核算的基数来源：基本工资统一以部门档案为准，普通员工取部门基础工资，部门经理取“部门基础工资 + 岗位工资”。
- [后端] 在算薪时自动回填员工档案 `baseSalary`，修复前端按部门展示、后端按员工旧快照计算导致的工资漂移。
- [后端] 加班工资统一改为读取考勤 `overtimeHours × 20`，考勤扣款优先读取考勤表已汇总的 `attendDeduct`，缺失时回退为 `lateTimes × 50`。
- [后端] 扣款合计补入 `otherDeduct`，重新对齐“实发 = 应发 - 社保 - 考勤扣款 - 其他扣款 - 个税”。

### PerformanceServiceImpl.java
- [后端] 绩效评分保存 / 修改后，不再只增量修补 `perfBonus`，改为触发当月薪资整单重算，确保绩效奖金、应发、个税、实发同步联动。

### EmployeeServiceImpl.java / EmployeeController.java
- [后端] 员工新增、修改、Excel 导入时统一按部门信息回填基础工资，经理记录自动叠加岗位工资。
- [后端] 修正员工修改接口直接 `updateById` 的旁路问题，改为走 `employeeService.updateEmployee()`，确保部门工资联动逻辑生效。

### admin/index.html
- [前端] 修复考勤数据、薪资核算等详情弹窗的中文副标题分隔符与头像展示，避免继续出现空白头像和残留英文风格。
- [前端] 将薪资核算 / 薪资发放表格中的“其他补助”统一更名为“津贴”，并同步更新图表 / 导出字段名称。
- [前端] 薪资核算、薪资发放列表的扣款金额统一纳入 `otherDeduct`，详情弹窗新增“其他扣款 / 扣款合计”展示。
- [前端] 考勤详情补充加班时长字段，为薪资核算中的加班工资来源提供直接可视化依据。

## 2026-04-02 V13.15（后台首页月份映射语法错误修复）

### admin/index.html
- [修复] 修正 `monthMap` 对象中因乱码导致的属性值未闭合与语句中断问题，手动回正 1-12 月中文文案，解决 `',' expected` 导致的 Vue 挂载失败。
- [修复] 清理个税明细与社保调整弹窗标题中残留的“路”字乱码，统一回正为标准间隔符 `·`。

## 2026-04-02 V13.14（后端控制器语法错误修复）

### SocialSecurityConfigController.java
- [修复] 修正 `SocialSecurityConfigController` 类尾部错误的 `import` 语句位置，将其移至文件顶部。
- [修复] 确保类大括号正确闭合，彻底解决编译报错。

## 2026-04-02 V13.13（项目工程规范化：.editorconfig 落地）

### 根目录
- [规范] 新增 `.editorconfig` 配置文件，从 IDE 底层强制约束项目中所有文件的编码为 UTF-8、换行符为 LF、缩进为 4 空格。
- [规范] 针对 Markdown 文件保留行尾空格，防止排版错位。
- [说明] 此举旨在彻底解决 AI 协作与跨系统操作中可能出现的 GBK/UTF-8 编码冲突和 CRLF 换行符干扰（对应解决任务 16 的根本防御）。


## 2026-04-02 V13.12（后台乱码清尾与导出文案回正）

### admin/index.html
- [前端] 继续清理 `admin/index.html` 的残余中文乱码，补齐员工、个人中心、支付网关、系统简介、公告、考勤规则、薪资结构、考勤录入、工资个税与社保等弹窗的标题、标签、说明与按钮文本。
- [前端] 回正考勤、公告、异常、绩效、员工删除等关键操作的确认提示、校验提示和成功/失败反馈，避免界面正常但交互消息仍是乱码。
- [前端] 修复导出文件名、导出表头、菜单标签、首页图表标题和默认站点文案，完成后台高频可见文本的整体验收收口。
- [前端] 补掉 `setup()` 返回对象区域残留的“考勤”乱码注释，继续按 UTF-8 标准收尾后台页面文本。

## 2026-04-01 V13.11（中文乱码恢复与详情文案回正）

### DEV_PLAN.md / CHANGELOG.md
- [文档] 恢复两个状态文件中被错误写回的中文乱码，重新整理为可正常阅读的 UTF-8 中文内容。

### admin/index.html
- [前端] 批量修复 `admin/index.html` 中因编码错写导致的中文乱码，优先恢复结构未变区域的标题、表头、菜单、按钮与表单标签。
- [前端] 手动修正管理员信息、个人中心、部门经理、考勤数据、考勤申请、绩效详情、薪资核算、工资个税与社保明细等高频入口的中文文案。
- [前端] 将考勤数据详情、考勤申请详情、绩效评分详情、薪资核算明细等统一详情模板文案切回中文，避免继续出现英文 eyebrow 与乱码混杂。
- [说明] 当前 `admin/index.html` 中仍保留少量低频注释 / 次级说明文字待继续清理，但主要页面与核心弹窗已恢复到正常可读状态。

## 2026-04-01 V13.10（处理类弹窗统一收尾）

### admin/index.html
- [前端] 新增考勤申请审批弹窗 `applyReviewDialogVisible`，替换原先的 `ElMessageBox.prompt` 审批方式，统一为正式 `modern-dialog` 审批流。
- [前端] 将异常处理、投诉反馈、绩效评分三类弹窗补齐统一的 Hero 信息区、柔和标签、卡片化表单区和一致的底部操作区，继续收口“考勤申请之后”的旧式处理窗口。
- [前端] 新增 `applyReviewContext`、`anomalyContext`、`feedbackContext`、`perfContext` 等上下文状态，保证不改原有接口与字段绑定的前提下提升详情 / 编辑弹窗的层级和复用性。

## 2026-04-01 V13.9（首页月份选择器与数据扩展）

### admin/index.html
- [前端] 首页新增月份选择器能力，在首页统计区加入 `el-date-picker`，默认展示当前月份，可切换查看不同月份的图表与统计数据。
- [前端] 调整 `loadDashboard` 逻辑，新增 `dashboardYearMonth` 响应式状态，优先使用用户选择月份；月份变化时自动重新加载首页数据。
- [前端] 简化首页日期计算逻辑，将原先较难维护的内联日期推导改为清晰的条件分支，修复浏览器解析异常。

### 数据准备
- [数据] 补充首页月份统计联调所需的测试数据，用于验证不同月份下的薪资、考勤和发放统计效果。

## 2026-04-01 V13.8（详情模板扩展与弹窗风格统一）

### admin/index.html
- [前端] 将剩余的详情 / 修改 / 处理弹窗统一切到 `modern-dialog / detail-dialog` 模板，包括个人资料、支付网关、关于我们、轮播图、系统简介、公告、考勤规则、薪资结构、考勤录入、异常处理、投诉反馈、绩效评分等入口。
- [前端] 新增通用 `detailDialogVisible / detailDialog / openDetailDialog` 详情模板状态与构造函数，替换公告、考勤数据、考勤申请、异常上报、绩效评分、薪资核算等模块原先的 `ElMessageBox.alert` HTML 字符串详情窗。
- [前端] 员工管理、部门经理、考勤申请、工资个税等页面统一补齐 `el-image` 头像预览链路；工资个税表新增头像列，并在 `taxDisplayRows` 中通过 `empId -> allEmployees` 关联头像字段。
- [前端] 修正工资个税页社保四项展示算法：优先基于 `baseSalary` 正向计算；缺失时仅用 `socialSecurityEmp / 0.175` 反推展示基数，避免 `0 / null` 时的展示异常。

## 2026-04-01 V13.7（详情模板收尾、头像预览与表格遮挡修复）

### admin/index.html
- [前端] 将员工详情 / 编辑弹窗的眉标题从英文统一切回中文，同时把详情头图与编辑区头像预览统一升级为 `el-image` 可点击放大，保持模板风格一致。
- [前端] 统一员工管理与部门经理列表的右侧固定操作列：两处表格均保留 `fixed="right"`，在窄页面或横向滚动时确保“操作”列始终可见。
- [前端] 补齐全局头像与缩略图预览链路：员工管理、部门经理、考勤申请、个人中心，以及关于我们 / 轮播图 / 系统简介 / 公告封面等图片统一追加 `preview-src-list` 与预览 Teleport。
- [前端] 将公告详情从旧的 `ElMessageBox.alert` HTML 拼接窗切换到统一的 `detail-dialog` 模板，并扩展模板支持图片型字段，便于后续复用到更多详情页。

## 2026-04-01 V13.6（详情弹窗模板统一与员工档案弹窗重构）

### admin/index.html
- [前端] 新增统一详情弹窗样式体系，以 `detail-dialog / detail-shell / detail-hero / detail-card / detail-descriptions` 为核心，固化 Profile 头部、大头像、柔和标签、扁平卡片与弱化标签 / 强化值的层级方案。
- [前端] 重构员工详情 / 修改弹窗：保留原有 `empForm` 字段与保存逻辑不变，新增详情态与编辑态分离。详情态改为 `el-descriptions` 展示，编辑态改为双栏分组表单布局。
- [前端] 将原先使用 `ElMessageBox.alert` 拼接 HTML 的详情入口升级为统一详情模板，覆盖考勤数据、考勤申请、绩效评分、薪资核算等模块，避免继续出现简陋文本详情窗。
- [前端] 同步调整员工管理、部门经理列表中的【详情】与【修改】入口，显式区分只读详情与编辑态，便于后续模块沿用同一结构继续扩展。

## 2026-04-01 仓库卫生：根目录视频不入库

### Git / GitHub
- 根目录下的演示 / 录屏类视频（`*.mp4` 等）不再纳入版本控制，并在 `.gitignore` 中按根路径忽略，避免误提交与超过 GitHub 单文件约 100MB 限制。
- 本地仓库 `user.email` 已与 GitHub 主邮箱对齐为 `614377781@qq.com`（便于提交记录与账户关联）。
- 为避免首笔提交历史中仍含超大 MP4 导致远程拒绝，已用 **orphan 单根提交**（`aed3140`）重写本地 `main`，并 **`git push --force` 同步至** `https://github.com/qimingnan463014/bishe`（覆盖原仅有 README 的远程初始提交）。
- 远程提示：根目录 **`uv.exe` 约 64MB**，低于 GitHub 单文件硬上限，但超过 50MB 建议值；若需减轻告警，可改为本机安装工具链或改用 Git LFS，不建议继续扩大二进制体积。

---

## 2026-04-01 V13.2（工资个税界面修复与弹窗美化）

### 全局头像系统底层修复与初始化
- **配置修复**：修复 `com.salary.config.WebMvcConfig` 中静态资源代理异常。给 `user.dir` 获取的绝对路径统一补全 `file:` 前缀与尾部斜杠，彻底解决图片保存成功但前端拉取报 404 / 加载失败的问题。
- **UI & DB 占位符补全**：通过自动化脚本批量访问 Dicebear 开放 API，按 `t_employee` 表内员工的 `real_name` 种子生成专属 SVG 头像，保存至底层 `uploads/avatar/` 目录，并同步回写数据库。

### 全局底层修复：MySQL 关键字 `year_month` 转义防御
**修改**
- [后端] 解决全量统计 / 分页接口中因 `year_month` 是 MySQL 保留关键字引发的 `SQLSyntaxErrorException` 500 报错。
- [后端] 为 `SalaryRecord`、`Performance`、`AttendanceRecord`、`SalaryPayment`、`TaxAccumulate` 等所有含该字段的实体强制添加 `@TableField("\`year_month\`")` 显式转义映射。
- [后端] 扫描并替换 `src/main/resources/mapper/` 目录下所有 XML 映射文件中手写的 `year_month` 为带反引号的格式。

### 工资个税与社保界面三项修复
**修改**
- [前端] 将 `tax` 表格的 `table-card` 改为 `compact-table-card`，与员工 / 部门经理等宽表保持一致。
- [前端] 重写 `taxDisplayRows` 中经理信息回溯逻辑：原来仅依靠 `allEmployees.userId` 匹配（经理未在 `allEmployees`），改为三级多源回溯策略（行自带字段 → `deptManagers` 列表按 `deptId / empNo` 查 → `departments.managerId + deptManagers` 联查），彻底修复部门经理与账号显示 `-` 的问题。
- [前端] 新增全局弹窗美化样式体系（`.modern-dialog`、`.dialog-header-gradient`、`.detail-card-grid`、`.detail-item`），让弹窗渐变 Header + 卡片 Grid 布局取代原来的 `el-descriptions`，同时修改弹窗为双栏 Row:gutter 对齐格式，整体视觉更简洁专业。

---

## 2026-04-01 V13.5（核心财务核算逻辑与论文双向对齐）

### 全新四重累加分绩效算法落地
**背景**：论文需求要求将各项打分具体化、金额配置化，并废弃“平均分算法”。
- **后端架构升级（`PerformanceServiceImpl.java` & `Performance.java`）**：
  - 复用冗余字段 `bonusDeduct` 作为【员工考勤分（0-30）】。
  - 重写 `autoCalcScoreAndGrade` 方法。最新记分公式：`工作表现(50) + 业务技能(10) + 工作态度(10) + 员工考勤(30) = 总得分(100)`。
  - 新增 Transient 承载字段 `qualifiedBonus`、`goodBonus`、`excellentBonus`，用于接收前端管理员自定义的奖金数额。
  - 评级区间从硬编码系数改为阶梯：`<61（不合格 / 0元）`、`61-75（合格）`、`76-85（良好）`、`86-100（优秀）`。
  - 将映射得出的最终【绝对金额】存入原本用于存比例的 `perfBonusRatio` 字段。
- **算薪直连提取（`SalaryServiceImpl.java`）**：
  - 核心算薪函数 `calculateSalary` 删除原有 `部门基数 × 20% × 绩效系数` 的绕弯逻辑，直接读取 `perfBonusRatio` 并赋值为当月绩效奖金 `perfBonus`。
- **UI 控制塔开放（`admin/index.html`）**：
  - 在【绩效评分】弹窗中暴露奖金配置输入框（默认 `900 / 1200 / 1500`），评分即录入，所见即所得。

### 五险一金正向基数切片算法落地
**背景**：原版展示错位，甚至前端尝试逆向推算，不符合系统严谨性。
- **前端重算渲染（`admin/index.html`）**：
  - 重构个税界面的推算能力。所有险种严格由 `baseSalary` 提取，不再通过合计包 `socialSecurityEmp` 逆推。
  - 彻底对齐论文标准：养老保险（8%）、医疗保险（2%）、住房公积金（7%）、失业保险（0.5%）。
  - 补全之前表格和弹窗丢失的经理姓名（`managerName`）与经理账号（`managerNo`）显示。
  - 新增【失业保险】数据列及对应的修改项。

## 2026-04-01 V13.4（绩效评分模块字段链路修复）

### application.yml：致命 Bug 修复
**根因：MyBatis-Plus 全局逻辑删除字段配置导致所有分页查询失败**
- 全局配置了 `logic-delete-field: deleted`，但项目中**所有数据表均无 `deleted` 列**。
- MyBatis-Plus 自动在每条 `SELECT` 末尾追加 `WHERE deleted = 0`，造成 SQL 报错 `Unknown column 'deleted'`，接口返回 500 并静默失败，前端表格显示“无数据”。
- **修复**：删除 `application.yml` 中 `logic-delete-field / value / not-delete-value` 三行配置。重启后端服务即可。
- **影响范围**：绩效评分、考勤、薪资核算、投诉反馈等所有使用 MyBatis-Plus `selectPage` 的接口均受益。

### 实体类映射：SQL 语法错误致命修复
**根因：MySQL 8 保留关键字冲突**
- MySQL 8.0 将 `year_month` 设为保留关键字（用于 `INTERVAL 1 YEAR_MONTH` 等）。
- MyBatis-Plus 直接生成 `SELECT id, year_month FROM ...` 导致报 500 `java.sql.SQLSyntaxErrorException`（被前端拦截为静默失败显示无数据）。
- **修复**：对涉及该字段的 7 个实体类（`Performance`、`SalaryRecord`、`AttendanceRecord`、`SalaryFeedback`、`SalaryPayment`、`TaxAccumulate`、`AnomalyReport`）统一使用 `@TableField("\`year_month\`")` 进行反引号转义。

### admin/index.html
**Bug 修复：绩效评分页面数据显示空白**
- **根因**：前端 `performanceDisplayRows` 映射和表格 `prop` 使用的字段名（`attendanceScore / attitudeScore / skillScore / performanceScore / bonusPenaltyScore`）与后端 `Performance.java` 实体类实际字段名（`workAttitude / businessSkill / workPerformance / bonusDeduct`）严重不匹配，导致后端返回的数据字段全部被丢弃为 `-`，表格空白。
- **修复范围**：
  1. `performanceDisplayRows` computed 映射字段名对齐
  2. 表格 `<el-table-column>` 的 `prop` 属性全部修正
  3. `calcPerfTotal()` 计算公式修正：改为三项平均分 + 奖惩加减分（与后端 `PerformanceServiceImpl` 实际逻辑一致）
  4. `openPerfForm()` 初始化默认值字段名修正
  5. `savePerformance()` POST / PUT 请求体字段名修正
  6. `openPerformanceDetail()` 详情弹窗字段名全部修正
- **附加**：grade tag 颜色同时兼容中文（优秀 / 良好 / 中等）和英文字母（S / A / B / C）两套旧数据格式。

---

## 2026-03-31 V13.3（交互与导出专项修复）

### admin/index.html
**1. 薪资反馈模块功能恢复**
- 修复“薪资反馈”菜单点击后页面出现空白的问题。原因是对应页面区块遗漏了对 `currentMenu === 'salary-feedback'` 的判断，导致页面组件被隐藏。

**2. 核心导出功能升级为 Excel（.xlsx）**
- 将全系统的 CSV 导出（包括考勤、薪资、绩效、发放等 6 处）统一切换为基于 `xlsx.full.min.js` 的标准 Excel 导出，避免跨平台 CSV 字符乱码和格式折损。

**3. 薪资核算默认月份体验优化（经修正为勾选行核算）**
- 修正 `batchCalcSalary` 逻辑：不再全局强推算某月，而是要求先勾选具体行后再逐行下发核算指令，支持细颗粒度重算。

---

## 2026-03-31 V13.2

### admin/index.html

**修复：操作按钮颜色不显示**
- 全局去除按钮 `type` 属性中的多余反斜杠（`type=\"warning\"` → `type="warning"`）
- 影响范围：部门、部门经理、员工、通知、考勤规则、薪资结构、考勤数据、考勤申请、异常上报、投诉反馈、工资个税、绩效评分、薪资核算、薪资发放等所有操作按钮
- 现在正确显示蓝（primary）/ 橙（warning）/ 红（danger）/ 绿（success）色

**统一分页组件（8 个模块）**
- 考勤数据、考勤申请、异常上报、投诉反馈、工资个税、绩效评分、薪资核算、薪资发放
- 全部改为 `v-model:current-page` + `v-model:page-size` + `@size-change`，layout 统一为 `"total, sizes, prev, pager, next, jumper"`
- 将 8 个 `xxxSize` refs 加入 `setup()` return，可在模板中直接访问

---

## 2026-03-31（hotfix）

### 白屏修复：`p` 变量未定义导致 Vue 实例挂载失败（V13.1-hotfix）
**根因**
- V13.1 全局 UI 重构时，`setup()` 的 `return { p, userInfo, ... }` 中引用了 `p`，但忘记在函数体内声明 `const p = reactive({})`。
- 这导致 JS 运行时抛出 `ReferenceError: p is not defined`，整个 `setup()` 崩溃，Vue 实例无法挂载，页面仅剩 `body` 浅蓝背景色。

**修复**
- `admin/index.html`：在 `setup()` 顶部补充声明 `const p = reactive({});`。

**第二轮 hotfix**
- `p` reactive 对象改为包含所有模板用到的分页子对象（`departments / deptManagers / carousel / systemIntro / systemLogs`），每个初始化为 `{ current: 1, size: 10 }`，修复 `Cannot read properties of undefined (reading 'current')` 渲染崩溃。
- 补充 `openSinglePaymentGateway(row)` 单笔发放弹窗函数，并加入 `return` 暴露给模板，修复薪资发放页点击【发放】按钮报 `is not a function` 的问题。

---

## 2026-03-31

### 全局 UI 与表格组件重构（V13.1）
**修改与新增**
- [前端] 统一规范全站所有 `<el-table>` 内部 `<el-button>` 的色值体系，统一为：详情 / 新增 / 普通操作（`primary`）、修改 / 编辑（`warning`）、删除 / 解散 / 驳回（`danger`）、审核 / 下发 / 通过（`success`）。
- [前端] 针对原来缺失分页组件的数据表格（部门、部门经理、轮播图管理、系统简介、系统日志），使用 Vue3 的 `reactive` 注入统一的纯前端分页切片能力 `p` 并挂载对应 `<el-pagination>`。

### 基建外挂扩充：开发规范技能库挂载
**新增**
- [规范] 挂载 `frontend-design-sop.md` 技能库：提炼 Anthropic 最佳实践，定义 Vue3 + Element Plus 的现代理念、色彩、间距等核心 UI 设计避坑体系。
- [规范] 挂载 `office-docs-sop.md` 技能库：打通 Java 报表导出（EasyExcel / POI-TL / iText）与前端 Vue3 Blob 流安全截获处理标准。

### 薪资发放流程闭环上线（V13.0）
**新增**
- [前端] 完善 `admin/index.html`，新增 `auditSalaryRecord`（单笔二审）、`batchAuditSalary`（批量二审）以及对应的 UI 按钮，在薪资列表中加入 `calcStatus` 状态显示。
- [前端] 新增针对薪资发放的聚合支付交互（`paymentGatewayVisible`、`paymentProcessing`），支持网银、支付宝企业版、微信商户代发特效选择。
- [后端] `SalaryController` 新增 `/audit`（单笔）和 `/batch-audit`（批量）接口。
- [后端] 修改 `SalaryServiceImpl` 中的 `paySalary` 接口，严格拦截至只有 `calcStatus == 3`（已审核）的记录才可发放，彻底闭环资金安全漏洞。

**状态机制**
- 完善从“草稿（1）→ 已发布待审（2）→ 已审核（3）→ 已发放（4）”的不可逆状态流转门控。

### 迁移记录（来自原计划文件）
- 迁移来源：`管理员界面差距修复跟踪.md` 的“变更记录”段落。
- 新建跟踪文件：`管理员界面差距修复跟踪.md`
- 写入当前发现的差异清单（1~5 项暂未完成修复）
- 约定：后续用于分析 / 调试的临时脚本或文件（例如一次性视频抽帧脚本）使用完毕后必须删除，不长期留在仓库里，避免污染项目结构。

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
- 支付界面显示银行、支付宝、微信选项。
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
## 2026-04-07 V13.73（个税累计预扣链路纠偏）
### salary-system/src/main/java/com/salary/service/impl/SalaryServiceImpl.java
- [后端] 个税累计预扣仍保持“`5000/月起征额 + 五险一金税前扣除 + 七级累进税率`，暂不考虑专项附加扣除”的业务口径，但修正了累计记录落库错误：`accumTaxableIncome` 现在保存真实累计应纳税所得额，不再错误缺少累计减除费用。
- [后端] `t_tax_accumulate` 的本月扣除拆分改为按工资基数回填 `monthFund` 与 `monthSocialSecurity`，修复历史上“公积金恒为 0、社保金额虚高”的问题。
- [后端] 本月应纳税所得额落库改为 `max(应发工资 - 五险一金 - 5000, 0)`，避免月度展示继续出现负税基。
### salary-system/src/main/resources/mapper/TaxAccumulateMapper.xml
- [后端] 查询累计记录时改为读取“本税年截至上月最近一条记录”，修复中间账期漏算后后续月份累计个税被错误重置的问题。
### salary-system/src/main/java/com/salary/entity/TaxAccumulate.java
- [后端] 同步修正累计字段注释，明确 `accumSocialSecurity` 当前保存的是“年度累计个人五险一金税前扣除（含公积金）”，避免后续继续按错误语义维护。
