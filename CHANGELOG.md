# CHANGELOG
## 2026-04-08 V14.27（GitHub 备份推送与薪资驳回链路补齐）
### 仓库 / GitHub
- [备份] 已先将当前系统代码快照整理后推送到 GitHub `main`，用于在补流程前保留一个可回退版本；本次推送未纳入本地 `docx`、视频、缓存、日志与截图目录。
### salary-system/src/main/java/com/salary/controller/SalaryController.java
- [后端] 新增薪资单笔驳回接口 `/salary/{id}/reject` 与批量驳回接口 `/salary/batch-reject`，仅允许管理员操作，并支持附带可选驳回原因。
### salary-system/src/main/java/com/salary/service/SalaryService.java
- [后端] 为薪资服务接口补充 `rejectSalary(...)` 与 `rejectBatch(...)` 定义，统一收口“待审核 -> 已驳回”的状态流转能力。
### salary-system/src/main/java/com/salary/service/impl/SalaryServiceImpl.java
- [后端] 将 `publishSalary(...)` 从“仅草稿可提交审核”扩展为“草稿或已驳回都可重新提交审核”，使经理在被打回后可修改并重新送审。
- [后端] 新增驳回实现：管理员可将 `2=待审核` 的工资单打回为 `5=已驳回`，并把驳回原因整理写入 `remark`，便于经理在列表/详情中查看。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 在薪资核算页新增“单笔驳回”“批量驳回”操作，管理员可对待审核账单直接退回经理修改。
- [前端] 为薪资状态筛选、列表状态标签、详情状态展示补齐 `5=已驳回`，并允许经理对“草稿/已驳回”账单都使用“编辑草稿”和“提交审核”。
- [前端] 将发放前端门控从原先的宽松判断修正为仅 `3=已审核` 才能发放，同时修复薪资发放表中 `4=已发放` 仍显示“已审核”的错误文案。
### 校验
- [校验] 已执行 `mvn -q -DskipTests compile`，后端编译通过。
- [校验] 已抽取并校验 `admin/index.html` 内联脚本语法，结果为 `admin inline script syntax ok`。
## 2026-04-08 V14.26（薪资核算流程图右侧回路连通修复）
### codexlunwen
- [图表] 继续微调 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中 `07-图3-7-薪资核算与审核发放流程图.png` 的右侧否分支回路，将其整理为完整连通的正交路径。
- [图表] 将“继续编辑草稿”与“经理编辑个别草稿”之间的连接改为同层水平箭头，同时将“是否提交审核”的否分支改为水平到框下方再竖直到框体，消除原先像断开的折线。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\07-图3-7-薪资核算与审核发放流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.25（登录流程图失败回路压缩为单拐点）
### codexlunwen
- [图表] 继续微调 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中 `06-图3-6-登录与角色分流流程图.png` 的左右失败回路，将原先仍偏复杂的路径压缩为单拐点后进入提示框。
- [图表] 保留“提示框同层水平返回输入框”的表达，只消除用户再次指出的多余拐弯，使失败回路更加干净。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\06-图3-6-登录与角色分流流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.24（登录流程图失败回路改为两段式路径）
### codexlunwen
- [图表] 继续微调 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中 `06-图3-6-登录与角色分流流程图.png` 的左右失败回路，将其改为“判断框先到提示框下方，再进入提示框”的两段式路径。
- [图表] 保留“提示框与输入框同层、提示框水平返回输入框”的表达，进一步贴合用户手绘示意，避免失败分支继续显得像直接贴边连入提示框。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\06-图3-6-登录与角色分流流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.23（登录流程图提示框改为同层水平回流）
### codexlunwen
- [图表] 继续微调 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中 `06-图3-6-登录与角色分流流程图.png` 的布局，将左右提示框整体上移到与“输入用户名、密码、验证码”同一水平层。
- [图表] 保留上一轮已确认的回流逻辑，但将“提示框 -> 输入框”显式改为同层水平箭头，消除用户指出的“为什么不直着过来”的视觉问题。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\06-图3-6-登录与角色分流流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.22（流程图 6/7 二次重排）
### codexlunwen
- [图表] 再次调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中 `06-图3-6-登录与角色分流流程图.png` 的布局，将两条失败回流改接到输入框左右侧，避免顶部主箭头与回流箭头继续重叠。
- [图表] 继续重排 `07-图3-7-薪资核算与审核发放流程图.png`，将“继续编辑草稿”抬升到与“经理编辑个别草稿”同一横向层级，并修正“退回经理修改”与审核不通过支路的表达，消除用户指出的折弯绕回与圈线问题。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\06-图3-6-登录与角色分流流程图.png` 与 `E:\bishe\codexlunwen\assets\mermaid\07-图3-7-薪资核算与审核发放流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.21（流程图 6/7 改为正交箭头版）
### codexlunwen
- [图表] 继续调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，将 `06-图3-6-登录与角色分流流程图.png` 的两条失败回路统一改为横平竖直的正交箭头，不再混用无箭头折线。
- [图表] 同步重排 `07-图3-7-薪资核算与审核发放流程图.png`，将“继续编辑草稿”“退回经理修改”和“发放结束”链路统一改为正交箭头表达，补齐更完整的论文式流程结构。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\06-图3-6-登录与角色分流流程图.png` 与 `E:\bishe\codexlunwen\assets\mermaid\07-图3-7-薪资核算与审核发放流程图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.20（ER 图基数关系纠正）
### codexlunwen
- [图表] 继续修正 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中数据库总体 ER 图的基数标注，将 `员工-考勤记录` 调整为一对多，将 `员工-绩效记录` 同步调整为一对多。
- [图表] 按当前论文业务口径，将 `薪资记录-薪资反馈` 的关系标注收敛为一对一，避免论文图继续出现“一条工资单对应多条薪资反馈”的误导表达。
- [交付] 重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`，本轮仍未回嵌论文 `docx`。
- [风险] 已在 `DEV_PLAN.md` 新增待办，记录“若系统实现也要严格一单一反馈，则后端仍需补唯一约束或服务层校验”，防止论文图与实际系统约束继续偏离。

## 2026-04-08 V14.19（ER 图员工方框继续上移并拉大上下间距）
### codexlunwen
- [图表] 继续调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，将数据库总体 ER 图中的 `员工` 方框及其上下属性整体继续上移。
- [图表] 同步下调 `生成` 关系菱形与 `薪资记录` 模块的相对链路位置，显式拉大 `员工` 与 `薪资记录` 之间的垂直距离，保留前一轮已确认的直线连接规则。
- [交付] 重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`，本轮仍未回嵌论文 `docx`。

## 2026-04-08 V14.18（参考文献扩至24篇与 ER 图员工实体上移）
### codexlunwen
- [论文] 更新 `E:\bishe\codexlunwen\文献池清单.md`，将正式参考文献总数由 20 篇扩充为 24 篇，并维持外文文献仅 4 篇、中文文献 20 篇的口径。
- [论文] 同步修改 `E:\bishe\codexlunwen\论文正文草稿.md` 的 `1.2 国内外文献综述` 与参考文献列表，新增 `[21]-[24]` 四篇中文文献，并补充对应综述表述。
- [图表] 调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中数据库总体 ER 图的中心布局，将 `员工` 实体整体上移，并联动修正 `生成` 关系、上下属性与基数标注位置。
- [交付] 重新覆盖生成 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`；本轮仍未回嵌论文 `docx`。

## 2026-04-08 V14.17（ER 图中心连线改为单段直线）
### codexlunwen
- [图表] 继续微调 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，将 `08-图3-8-数据库总体-ER-图.png` 中 `绑定`、`归属`、`形成`、`参与` 与中心实体 `员工` 之间的连接线由折线改为单段直线。
- [图表] 保留上一轮已经确认的属性上下分流布局，只修正中心关系连接方式与基数标注位置，使 ER 图更贴近用户给出的论文式样例。
- [交付] 本轮重新覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.16（ER 图属性分流重排与参考文献外文压缩）
### codexlunwen
- [图表] 继续调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，将 `08-图3-8-数据库总体-ER-图.png` 中 `员工` 实体改为“上 3 下 2”的属性布局，将 `薪资记录` 实体改为上下分流属性布局，并重新整理关系线走向，避免线条继续压在属性区上。
- [图表] 重新生成 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`，本轮仍只更新 PNG，不回嵌论文 `docx`。
- [论文] 定点修正 `E:\bishe\codexlunwen\论文正文草稿.md` 的 `1.2 国内外文献综述` 与参考文献列表，删除多余外文文献引用，将外文文献压缩为 4 篇，并同步调整正文引用编号顺序。
- [论文] 同步更新 `E:\bishe\codexlunwen\文献池清单.md`，将正式采用口径改为“20 篇参考文献、外文 4 篇、中文 16 篇”，确保后续重新导出 Word 时不再回到外文过多的旧版本。

## 2026-04-08 V14.15（权限结构图改 H 图，ER 图补足核心属性）
### codexlunwen
- [图表] 继续调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，将 `02-图3-2-权限与角色结构图.png` 从普通树状权限图改为与图 3.1 同风格的 H 图表达，角色主框与子功能框均改为竖排窄框。
- [图表] 将 `08-图3-8-数据库总体-ER-图.png` 从压缩版属性图扩充为“核心属性增强版”，按真实表结构补充了用户账号、员工、部门、考勤记录、绩效记录、薪资记录、薪资反馈、公告等实体的关键字段。
- [图表] 本轮继续沿用本机 Pillow 脚本直接输出成品图片，生成物为磁盘中的 `PNG` 文件，不是 `draw.io/.drawio` 源文件，也不是截图。
- [交付] 本轮仅覆盖更新 `E:\bishe\codexlunwen\assets\mermaid\02-图3-2-权限与角色结构图.png` 与 `E:\bishe\codexlunwen\assets\mermaid\08-图3-8-数据库总体-ER-图.png`，未回嵌论文 `docx`。

## 2026-04-08 V14.14（图片确认前仅更新 PNG，不回嵌论文）
### codexlunwen
- [流程] 根据用户最新要求，后续图片调整阶段先只更新 `E:\bishe\codexlunwen\assets\mermaid\*.png`，暂不继续重建主论文 `docx`，待用户确认图片整体通过后再统一嵌回正文。
- [图表] 本轮继续修正 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py` 中两张问题图：`06-图3-6-登录与角色分流流程图.png` 与 `08-图3-8-数据库总体-ER-图.png`。
- [图表] 登录流程图重新布线，避免左右提示框与连接线压边重叠；ER 图将员工实体属性区与关系线拆开，并将薪资记录属性移到实体上方，整体关系连接重新调整到实体左右边。
- [交付] 本轮仅覆盖更新 PNG，未重新生成 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现.docx`。

## 2026-04-08 V14.13（论文图表二次修正：竖排功能模块图与去框用例图）
### codexlunwen
- [图表] 根据用户进一步反馈，继续调整 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`：将功能模块图改为一级模块、二级模块均为竖排窄框，子模块由竖向堆叠改为每个一级模块下横向展开，贴近样例中的论文结构图风格。
- [图表] 去掉管理员、经理、员工三张用例图中的系统边界外框，同时去掉演员姓名外部矩形标签，只保留小人和文字，收敛为用户指定的简洁 UML 用例图样式。
- [图表] 修正登录流程图，删除多余的“管理员登录”过程框，使流程从“开始”直接进入“输入用户名、密码、验证码”，并保留后续空值校验、正确性校验、成功结束的标准逻辑。
- [图表] 重画数据库 E-R 图布局，改为围绕系统真实实体 `用户账号/员工/部门/考勤记录/绩效记录/薪资记录/薪资反馈/公告` 的原创 Chen 风格结构，避免继续出现“像直接照着样图复制”的观感。
- [交付] 覆盖更新 `E:\bishe\codexlunwen\assets\mermaid` 下对应 PNG，并重新生成主论文 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现.docx`；校验结果仍为 `media=8`、`drawings=8`。

## 2026-04-08 V14.12（论文图表改为标准论文图范式）
### codexlunwen
- [图表] 根据用户提供的样例图，确认上一轮图片仍偏 Mermaid 工程示意图，不符合“功能模块图/标准 UML 用例图/标准流程图/Chen 风格 ER 图”的论文配图要求。
- [图表] 新增绘图脚本 `E:\bishe\codexlunwen\tools\draw_thesis_diagrams.py`，直接用本机 Pillow 生成黑白论文图，统一覆盖图 3.1 至图 3.8 对应 PNG。
- [图表] 本轮功能模块图改为树状矩形分层结构，用例图改为“小人 + 系统边界 + 椭圆用例”，流程图改为开始/处理/判断标准形状，ER 图改为实体矩形、关系菱形、属性椭圆的经典表达方式。
- [版式] 去除图片内部自带图号标题，避免与 Word 正文自动保留的图题重复；同时上移用例图系统边界标题，避免压住用例椭圆。
- [交付] 重新生成主论文 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现.docx`，并确认 `media=8`、`drawings=8`，8 张新版论文图已重新嵌入正文。

## 2026-04-08 V14.11（论文第3章 UML/功能模块/ER 图重绘并回嵌正文）
### codexlunwen
- [图表] 定点更新 `E:\bishe\codexlunwen\论文正文草稿.md` 第 3 章中的图 3.1、图 3.3、图 3.4、图 3.5、图 3.8，将原先偏简单的 Mermaid 结构图重绘为更贴近论文风格的功能模块图、三角色 UML 用例图和带关键字段的数据库总体 ER 图。
- [图表] 同步更新 `E:\bishe\codexlunwen\图表清单与说明.md` 中对应 5 张图的源码说明，保证图表清单、正文源码和最终成图口径一致。
- [渲染] 重新抽取 Mermaid 源块并覆盖 `E:\bishe\codexlunwen\assets\mermaid\*.mmd` 与对应 PNG 成图，保留原图号与图片映射关系不变。
- [交付] 重新生成主论文 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现.docx` 与附件 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现-附件.docx`。
- [校验] 已确认新主论文中 `media=8`、`drawings=8`，说明 8 张图已实际嵌入 Word 正文而非仅停留在磁盘图片文件。

## 2026-04-08 V14.10（图表 MCP 安装与多 Agent 共享接入）
### Codex / MCP / Skills
- [安装] 通过 `npx.cmd -y plantuml-mcp-server --help` 与 `npx.cmd -y mcp-mermaid --help` 预拉取并验证了 PlantUML 与 Mermaid 两套图表 MCP 依赖，确认本机可正常启动。
- [配置] 更新用户级配置 `C:\Users\614377781\.codex\config.toml`，新增 `[mcp_servers.plantuml]` 与 `[mcp_servers.mermaid]`，使后续新开的 Codex 会话和新建 agent 可直接继承图表能力；同时保留备份文件 `C:\Users\614377781\.codex\config.toml.bak-20260408-diagram-mcp`。
- [技能] 新增工作区技能说明 `E:\bishe\.agents\skills\diagram-mcp\SKILL.md`，约定 UML/用例图/流程图优先走 PlantUML，功能模块图/HIPO 图优先走 Mermaid，便于本项目中的其他 agent 统一出图口径。

## 2026-04-08 V14.09（论文图表黑白直线风格优化）
### codexlunwen
- [图表] 定点调整 `E:\bishe\codexlunwen\论文正文草稿.md` 第 3 章 8 张 Mermaid 图的初始化配置，统一切换为纯黑白极简风格，并为 `flowchart` 启用 `linear + elk` 布局，尽量将连接线收敛为横平竖直的正交样式。
- [图表] 保持摘要正文不整页重写，仅保留此前已修复的中文摘要措辞，并继续沿用新的 HIPO 图、用例图、标准流程图和 ER 图结构。
- [交付] 重新渲染 8 张系统图并输出新文档 `E:\bishe\codexlunwen\thesis-figure-bw-straight.docx`。
- [校验] 已确认新文档中 `media=8`、`drawings=8`，8 张黑白直线风格图均已嵌入正文。

## 2026-04-08 V14.08（论文摘要措辞修正与第3章图表重绘）
### codexlunwen
- [论文] 定点修正 `E:\bishe\codexlunwen\论文正文草稿.md` 中文摘要表述，去除摘要中的“了”字，保留原有语义但收紧为更符合毕业论文摘要的书面口吻。
- [图表] 按论文图表示例重绘第 3 章 Mermaid 图：将 `图3.1` 调整为系统总体 HIPO 功能图，将 `图3.3/3.4/3.5` 调整为管理员、部门经理、员工功能用例图，将 `图3.6/3.7` 调整为标准流程图样式。
- [图表] 同步更新 `E:\bishe\codexlunwen\图表清单与说明.md`，使图名、图型和正文引用保持一致。
- [交付] 重新渲染 8 张图并生成新论文文件 `E:\bishe\codexlunwen\thesis-figure-rework.docx`；校验结果为 `media=8`、`drawings=8`、`pageBreakBefore=8`，可作为当前新的带图修正版。

## 2026-04-08 V14.07（论文 DOCX 分页空白修复与带图版重出）
### codexlunwen
- [论文] 复查发现 `template_styles.json` 中 `body_cn`、`body_en` 被样式分析结果误写成 `page_break_before=true`，导致 Word 正文几乎每段都强制另起一页，出现大面积空白并影响图文连续展示。
- [论文] 定点修正正文样式分页参数后，重新生成带图论文，新增成品 `E:\bishe\codexlunwen\thesis-fixed-images.docx` 与 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现（分页修复带图版）.docx`。
- [校验] 修复后的新文件均已确认包含 8 张内嵌图片与 8 处正文绘图引用，`pageBreakBefore` 从旧版的 128 处降到 8 处，恢复到章节级分页而非段落级分页。

## 2026-04-08 V14.06（论文终稿、图源码附件与参考文献清单交付）
### codexlunwen
- [论文] 基于已确认的目录、样式分析、系统事实底稿、图表清单和文献池，生成正式主论文文档 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现.docx`。
- [论文] 同步生成图表源码附件 `E:\bishe\codexlunwen\基于Java的员工薪资管理系统设计与实现-附件.docx`，收录正文中的 Mermaid 图源码，满足论文附件交付要求。
- [论文] 生成 `E:\bishe\codexlunwen\参考文献规范化清单.xlsx`，整理 22 篇候选参考文献的年份、作者、题名、文献类型、来源、DOI/URL、来源渠道与用途备注，便于后续答辩和格式复核。
- [图表] 已从正文草稿中提取 8 个 Mermaid 图块到 `E:\bishe\codexlunwen\assets\mermaid`；由于当前环境下 `npx @mermaid-js/mermaid-cli` 拉取依赖时报 `EACCES`，本轮未将 Mermaid 图片嵌入主论文，但未阻塞主论文与附件成品交付。
- [校验] 主论文 `.docx`、附件 `.docx` 已成功落盘；参考文献清单 `.xlsx` 已通过 `formula_check.py` 静态校验，确认工作簿结构正常、无公式错误。

## 2026-04-08 V14.05（经理个人中心取数恢复与员工端按钮类型修复）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 修复部门经理/管理员共享后台中 `loadSelfInfo()` 已损坏的个人中心取数逻辑，新增 `applySelfInfo()` 与 `syncSelfForms()`，统一用 `/api/auth/me` 返回值和当前登录态回填 `selfInfo`/`selfEditForm`。
- [前端] 为 `selfInfo` 补齐 `position`、`empId` 等字段映射，并在保存个人资料成功后同步刷新顶部登录态显示名称，避免经理个人中心再次出现整块 `-` 占位。
### salary-system/src/main/resources/static/home.html
- [前端] 修正修改密码按钮的 `type=" primary"` 为 `type="primary"`，消除员工端 Element Plus 按钮类型校验 warning。
- [校验] 重新执行经理账号登录与员工账号登录回归：经理 `10001/123456` 的个人中心已恢复显示工号、姓名、性别、手机、身份证号、部门、银行卡号、岗位与入职时间；员工页控制台新增 warning 为 0。

## 2026-04-08 V14.04（管理员端三张宽表铺满布局修复）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 修正“考勤数据”“薪资核算”“薪资发放”三张列表表格的列宽策略，保留现有视觉和字段顺序，仅将多列从固定 `width` 调整为 `min-width`。
- [前端] 为考勤表补充可伸缩列，覆盖登记编号、月份、工号、姓名、部门、考勤统计、登记日期、经理信息等字段，消除右侧大块留白。
- [前端] 为薪资核算与薪资发放表补充可伸缩列，覆盖月份、工号、姓名、部门、银行卡、工资构成、税费、状态、发放日期与文件列，让宽表在大屏下自动铺满卡片容器。
- [校验] 变更后重新提取 `admin/index.html` 内联脚本执行语法检查，确认本轮模板层列宽调整未破坏后台页脚本执行。


## 2026-04-08 V14.03 (扩展外部技能库与科研插件集成)
### e:\bishe\.agents\skills
- [集成] 从 GitHub 引入 lunwen-skill (毕业论文撰写工具)、cnki-skills (知网文献检索插件) 及 gs-skills。
- [配置] 将上述技能全量安装至本地 Agent 技能库，使其在后续文档生成、报告撰写与学术校对任务中可被自动调用。

## 2026-04-08 V14.02（2026-01/2026-02 经理工资补齐与 1-3 月个税链路重建）
### 数据库 bishe
- [数据] 补齐 `2026-02` 缺失的 5 条经理工资单（`10001`-`10005`），直接复用已存在的 2 月经理绩效与考勤记录生成工资，统一补上 `attendance_id`、工资构成字段、发放状态与发放日期。
- [数据] 按当前后端累计预扣法重算 `2026-01` 到 `2026-03` 共 45 条工资单的 `income_tax`、`total_deduct`、`net_salary`，使工资表中的个税与实发金额和真实累计规则对齐。
- [数据] 清空并重建 `t_tax_accumulate` 中 `2026-01` 至 `2026-03` 的累计个税记录，共回写 45 条，补齐 `salary_record_id` 关联，并按基础工资拆分 `month_social_security` 与 `month_fund`。
- [数据] 为 `2026-01`、`2026-02` 的 30 条已发放工资单补建 `t_salary_payment` 记录；同时把 1-3 月已有支付记录的 `net_salary`、`pay_date` 同步到最新工资表口径。
- [核对] 修正后复查确认：`2026-01`、`2026-02`、`2026-03` 三个月的工资表、支付表、累计个税表均已达到每月 15 条；支付金额与工资表 `net_salary` 全量一致，工资公式校验无残留差异。

## 2026-04-07 V14.01（2026-03 薪资重复脏数据清理与发放链路回正）
### 数据库 bishe
- [数据] 复查 2026 年 1-3 月工资数据时，发现 `2026-03` 的 `t_salary_record`、`t_performance`、`t_attendance_record` 同月都存在重复记录：员工 `001-010` 各有两套 3 月数据，导致工资、绩效、考勤口径彼此打架。
- [数据] 直接清理旧的 3 月重复数据，删除 `t_salary_record` 旧工资单 `21-30`、`t_performance` 旧绩效 `36-45`、`t_attendance_record` 旧考勤 `36-45`，将 3 月三张业务表统一收敛回每人 1 条、共 15 条记录。
- [数据] 修正经理账号 `10001-10005` 的 3 月工资单：按当前部门基础工资/岗位工资、绩效奖金、考勤扣款、加班工资口径回写 `base_salary`、`overtime_pay`、`perf_bonus`、`social_security_emp`、`attend_deduct`、`gross_salary`、`total_deduct`、`net_salary`，并统一状态为已发放。
- [数据] 同步修正 `t_salary_payment` 的 3 月发放记录，将员工 `001-010` 的支付记录从旧工资单 `21-30` 回绑到保留的工资单 `51-60`，并把经理支付金额改到与工资表 `net_salary` 一致；同时删除重复支付记录 `31`、`32`。
- [数据] 同步修正 `t_salary_feedback` 中引用旧工资单 `21-30` 的 10 条反馈记录，统一改绑到保留的工资单 `51-60`，避免后续页面查询命中已删除工资单。
- [数据] 补正经理 `10003` 在 `t_attendance_record` 的 3 月考勤扣款，将迟到 1 次对应的 `attend_deduct` 调整为 `50.00`，与工资扣款口径保持一致。
- [核对] 修正后复查确认：`2026-03` 的工资、绩效、考勤、发放四张表均已回到 15 条有效记录；工资单无重复、发放金额与 `net_salary` 全量一致、反馈表不再引用已删除工资单。

## 2026-04-07 V14.00（管理员页薪资反馈头像兜底修复）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 修正管理员端“薪资反馈”列表 `feedbackDisplayRows` 的资料映射逻辑，不再只从 `allEmployees` 单点查找员工信息，改为统一复用 `resolveEmployeeProfile(...)`。
- [前端] 为薪资反馈列表补上 `getStoredAvatar(...)` 头像兜底链路，兼容普通员工与部门经理账号的头像回填，修复工号 `10001`、`10003`、`10004` 等记录只显示首字母头像的问题。
- [前端] 同步统一薪资反馈列表中的工号与姓名兜底逻辑，避免头像修复后仍出现账号信息缺失或不一致。
- [校验] 已对 `admin/index.html` 内联脚本执行语法检查，并重新执行 `mvn -q -DskipTests compile`，确认本次修复未破坏现有前后端链路。

## 2026-04-07 V13.99（2026-03 绩效记录补齐与工资单奖金回写）
### 数据库 bishe
- [数据] 发现工资相关月份已调整为 `2026-03` 后，`t_performance` 仍缺少对应月份的绩效评分记录，导致 3 月工资单绩效奖金全部显示为 `0`。
- [数据] 直接以现有 `2025-01` 的 15 条绩效记录为模板，复制生成 `2026-03` 的绩效评分数据，保留当前规则下的 `优秀/良好/合格/不合格` 及 `1500/1200/900/0` 奖金口径。
- [数据] 将 `t_salary_record` 中 `2026-03` 的工资单同步回写绩效奖金，并联动修正 `gross_salary`、`net_salary`，使后台“薪资核算”页不再出现 3 月绩效奖金整列为 `0` 的情况。
- [核对] 修正后复查确认：`t_performance` 中 `2026-03` 已存在 15 条绩效记录，奖金总额为 `14400.00`，工资表中的 3 月绩效奖金列也已恢复为非零显示。

## 2026-04-07 V13.98（工资月份回拨与重算状态保留修复）
### salary-system/src/main/java/com/salary/service/impl/SalaryServiceImpl.java
- [后端] 修正 `calculateSalary()` 的落库逻辑：仅新生成的工资单默认设为草稿并初始化工资条发布字段；对已存在的工资单执行重算时，保留原有 `calcStatus`、`slipPublished`、`slipPublishTime`、`recordDate`，避免联动绩效或月底重算时把已审核/已发放记录打回草稿。
- [校验] 调整后执行 `mvn -q -DskipTests compile`，确认后端编译通过。
### 数据库 bishe
- [数据] 直接使用 MySQL 将工资相关演示数据的月份从 `2026-04` 统一调整为 `2026-03`，覆盖 `t_salary_record`（15 条）、`t_salary_payment`（17 条）、`t_tax_accumulate`（5 条），使管理员端当前展示符合“4 月查看 3 月工资单”的业务口径。
- [核对] 调整后复查确认：三张表中的 `2026-04` 记录已清零，工资主表演示月份已统一为 `2026-03`。

## 2026-04-07 V13.97（绩效表结构扩容与历史绩效奖金数据回正）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 修正绩效评分列表中“评价等级”标签的文案判断，将旧判断里的“及格”统一改为当前规则使用的“合格”，避免等级颜色映射与后端实际等级值不一致。
### 数据库 bishe
- [结构] 将 `t_performance.grade` 从 `varchar(2)` 扩容为 `varchar(10)`，修复保存“不合格”评分时出现的 `Data too long for column 'grade'`。
- [数据] 将 `t_performance` 现有 15 条绩效记录统一按当前规则重算为 `优秀/良好/合格/不合格` 与 `1500/1200/900/0` 对应奖金金额。
- [数据] 将 `t_salary_record` 中可关联绩效记录的工资单同步到新的绩效奖金口径，消除 `perf_bonus` 与 `perf_bonus_ratio` 不一致的历史脏数据。
- [数据] 将没有绩效评分记录却仍残留旧绩效奖金的工资单清零绩效奖金，并同步重算 `gross_salary`、`total_deduct`、`net_salary`，避免旧规则奖金继续混入当前演示数据。
- [核对] 修正后复查确认：`t_salary_record` 与 `t_performance` 之间的绩效奖金不一致记录已降为 `0` 条。

## 2026-04-07 V13.96（后台图片预览层定位修复）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 修正后台页面所有启用了 `preview-src-list` 的 `el-image` 预览属性，将错误使用的 `teleported` 改为 `preview-teleported`，覆盖员工/经理头像、公告封面、详情头像、上传预览、税务头像等场景。
- [效果] 图片点击放大后，预览层改为挂载到 `body`，不再被表格、详情弹窗或局部容器裁切和定位，避免出现“放大图卡在表格中间”的异常展示。
- [校验] 修复后重新执行页面脚本语法检查，确认本轮调整未影响后台页现有功能。

## 2026-04-07 V13.95（经理薪资草稿区残留坏串补修）
### salary-system/src/main/resources/static/admin/index.html
- [前端] 继续清理经理薪资核算区域残留的 `草60` / `?..` 坏串，统一恢复“草稿”相关文案，包括状态筛选项、表格操作按钮、草稿配置/信息标题、生成与保存按钮、批量生成加载提示、生成失败与保存失败提示等。
- [校验] 修复后重新提取页面 `<script>` 做语法检查，确认本轮文本替换未破坏管理员/经理页脚本执行。

## 2026-04-07 V13.94（登录页与管理员页中文乱码回退修复）
### salary-system/src/main/resources/static/login.html
- [前端] 重新核对登录页中文文案与脚本结构，保留“去掉角色选择、按后端真实角色自动分流”的现有逻辑，确认页面未再引入新的乱码文本。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 对管理员/经理共用页中已损坏的中文做 UTF-8 定点修复，恢复首页“最新通知公告”、经理薪资区“生成草稿/添加草稿/提交审核”提示、个人中心“所在部门/请输入身份证号”、草稿弹窗标题、副标题、考勤统计“早退”、默认扣款规则、草稿生成与保存提示、数据备份权限提示等文案。
- [校验] 修复后重新提取 `login.html` 与 `admin/index.html` 的 `<script>` 做语法校验，并执行 `mvn -q -DskipTests compile`，确认本轮文本修复未破坏现有前端脚本与 Spring Boot 编译链路。

## 2026-04-07 V13.93（登录页角色选择移除与首页考勤饼图百分比显示）
### salary-system/src/main/resources/static/login.html
- [前端] 删除登录页手动角色选择下拉框，登录后继续仅按后端返回的真实 `role` 自动分流到管理员/经理端或员工端。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 将首页“当月整体考勤状态”饼图改为百分比展示，标签与 tooltip 统一显示占比；空数据时显示“暂无考勤数据”，不再直接显示容易误解的累计值。

## 2026-04-07 V13.92（考勤异常率重整、早退字段落地与扣款规则公告化）
### salary-system/src/main/java/com/salary/entity/AttendanceRecord.java
- [后端] 新增 `earlyLeaveTimes` 字段，和数据库 `t_attendance_record.early_leave_times` 对齐，避免早退继续混在迟到次数里。
### salary-system/src/main/resources/mapper/AttendanceRecordMapper.xml
- [后端] 统计接口 `countAttendanceStatus` 拆出“早退”维度，正常出勤改为扣除迟到与早退次数后再汇总。
### salary-system/src/main/java/com/salary/service/impl/AttendanceServiceImpl.java
- [后端] `fillAttendanceDeduct()` 改为按“迟到 + 早退”统一计次扣款；导入打卡机数据时继续单独识别早退并写入独立字段。
### salary-system/src/main/java/com/salary/service/impl/SalaryServiceImpl.java
- [后端] 薪资核算同步读取 `lateTimes` 与 `earlyLeaveTimes`，只更新考勤扣款与加班薪金相关字段，不额外改动累计个税链路。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理员/经理端考勤统计图、考勤规则、考勤数据列表、详情弹窗、编辑表单与导出字段全部补上“早退”；考勤规则增加明确扣款规则文案。
### salary-system/src/main/resources/static/home.html
- [前端] 员工端考勤表格、详情映射与打印模板补上“早退天数/早退次数”，保证员工查看和打印口径一致。
### 数据库 bishe
- [数据] 新增 `t_attendance_rule` 扣款相关字段并固化演示口径：迟到/早退 50 元/次，事假按日薪 100% 扣减，病假按日薪 50% 扣减，年假 5 天。
- [数据] 重整 2026-04 的 15 条考勤记录与对应薪资考勤字段，异常率提升到 `5.61%`，当前包含旷工 3 人、迟到 6 人、早退 7 人、请假 5 人。
- [数据] 新增置顶公告《2026年4月考勤扣款与异常认定说明》，用于答辩演示时统一说明扣款口径与异常认定标准。

## 2026-04-07 V13.91（头像静态放行、性别匹配重排与公告时间可视化）
### salary-system/src/main/java/com/salary/config/SecurityConfig.java
- [后端] 新增 `"/front_assets/**"` 匿名放行，修复登录后头像、公告封面、轮播图等静态图片请求被拦截后出现“加载失败”的问题。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 在管理员“通知公告”列表页增加“发布时间”列，保留按 `pubTime/createTime` 倒序的前端兜底逻辑，避免列表看起来像是未排序。
### salary-system/src/main/resources/static/front_assets
- [资源] 重新整理员工/经理头像素材，替换重复头像文件并新增 `male_avatar_5.jpg`、`male_avatar_10.jpg`，当前头像文件哈希已校验为无重复。
### 数据库 bishe
- [数据] 同步重排 `t_employee` 与 `t_user` 中 15 个员工/经理账号的头像路径，保证男/女性别与头像性别一致，且实际分配出去的头像不重复。

## 2026-04-07 V13.90（前端依赖本地化，离线可用）
### salary-system/src/main/resources/static/home.html
- [前端] 将员工门户页的 Vue、Element Plus、Axios 等依赖从公网 CDN 切换为本地 `/api/vendor/**`，避免清缓存后再次依赖外网下载。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 将管理员/经理入口页的基础运行库、本地图表与 Excel 依赖统一改为本地静态资源，保证后台主界面断网可打开。
### salary-system/src/main/resources/static/login.html
- [前端] 将登录页脚本与样式依赖切换到本地资源，避免登录页在弱网或离线时长时间空白。
### salary-system/src/main/resources/static/index.html
- [前端] 将员工端旧入口页的第三方依赖同步改为本地资源，消除多入口混用时的公网耦合。
### salary-system/src/main/resources/static/admin/css/login-font.css
### salary-system/src/main/resources/static/api/admin/css/login-font.css
### salary-system/src/main/resources/static/login_assets/font_4097802_w9071sf3dx.css
- [前端] 将 iconfont 字体地址从 `at.alicdn.com` 改为本地 `/api/vendor/iconfont/*`，修复清缓存或断网时图标丢失的问题。
### salary-system/src/main/resources/static/vendor
- [资源] 新增本地前端依赖目录，纳入 `vue.global.js`、`element-plus`、`axios.min.js`、`echarts.min.js`、`xlsx.full.min.js` 以及 iconfont 字体文件，作为统一离线资源源。
### salary-system/src/main/java/com/salary/config/SecurityConfig.java
- [后端] 新增 `"/vendor/**"` 静态资源放行规则，确保匿名访问页面时可直接加载本地化后的前端依赖。

## 2026-04-07 V13.89（头像资源恢复与公告倒序统一）
### salary-system/src/main/java/com/salary/service/impl/AnnouncementServiceImpl.java
- [后端] 公告分页排序改为严格按 `pubTime`、`createTime`、`id` 倒序，不再让置顶字段打乱“最新公告在最上”的展示预期。
### salary-system/src/main/resources/static/home.html
- [前端] 员工主页公告卡片与公告列表增加前端二次时间排序，确保首页和“通知公告”页都与后端保持同一倒序口径。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 管理端首页公告区与“通知公告”列表页同步增加按发布时间倒序的前端兜底排序，避免缓存或旧接口顺序导致列表看起来乱序。
### salary-system/src/main/resources/static/front_assets
- [资源] 将员工头像 `yuangong_touxiang1~8.jpg` 与经理头像 `bumenjingli_touxiang2~8.jpg` 从现有素材目录补回静态资源库，修复员工/经理表格头像大面积“加载失败”问题。
### 数据库 bishe
- [数据] 将员工 `009/010` 的头像改绑到未被经理实际占用的 `bumenjingli_touxiang6/7.jpg`，避免继续指向不存在的 `yuangong_touxiang9/10.jpg` 文件。

## 2026-04-07 V13.88（演示主页内容补齐与员工业务数据充实）
### salary-system/src/main/java/com/salary/service/impl/SysConfigServiceImpl.java
- [后端] 将当前工作区的系统配置实现同步回文件持久化版本，恢复 `/api/sys-config/website_content` 的可用性，避免员工首页和管理端网站内容再次因缺失 `t_sys_config` 表而失效。
### salary-system/src/main/resources/static/home.html
- [前端] 将员工首页恢复为带“关于我们详情页”的网站主页版本，首页卡片支持点击“查看详情”进入独立详情界面，继续保留公告、轮播图、系统简介三块门户内容。
### salary-system/src/main/resources/static/admin/index.html
- [前端] 将管理端网站配置同步逻辑恢复到服务端优先口径，并保留本地兜底补同步逻辑，避免后续维护首页内容时再次出现两端不同步。
### 数据库 bishe
- [数据] 为 `t_announcement` 现有公告统一补齐封面图，并新增 10 条更适合答辩演示的公告，当前公告总数扩充到 16 条。
- [数据] 为 `t_attendance_record` 新增 2026-03 月度考勤，并统一重整 45 条考勤记录的出勤、请假、加班、扣款和状态分布，当前异常率约为 4.44%，接近 5% 演示目标。
- [数据] 为 `t_attendance_apply`、`t_salary_feedback`、`t_anomaly_report` 按 `t_employee` 全量补齐 2026-04 月业务记录，3 类单据现均覆盖全部 15 个员工档案，方便员工端、经理端和管理员端演示查看与处理流程。

## 2026-04-01 仓库卫生：根目录视频不入库

### Git / GitHub
- 根目录下的演示/录屏类视频（`*.mp4` 等）不再纳入版本控制，并在 `.gitignore` 中按根路径忽略，避免误提交与超过 GitHub 单文件约 100MB 限制。
- 本地仓库 `user.email` 已与 GitHub 主邮箱对齐为 `614377781@qq.com`（便于提交记录与账户关联）。
- 为避免首笔提交历史中仍含超大 MP4 导致远程拒绝，已用 **orphan 单根提交**（`aed3140`）重写本地 `main`，并 **`git push --force` 同步至** `https://github.com/qimingnan463014/bishe`（覆盖原仅有 README 的远程初始提交）。
- 远程提示：根目录 **`uv.exe` 约 64MB**，低于 GitHub 单文件硬上限，但超过 50MB 建议值；若需减轻告警，可改为本机安装工具链或改用 Git LFS，不建议继续扩大二进制体积。

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
