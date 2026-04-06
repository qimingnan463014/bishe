-- =============================================================================
-- 基于Java的员工薪资管理系统 - 数据库设计（第三范式）
-- 数据库：MySQL 8.x
-- 优先级：开题报告字段定义 > HTML页面字段提取 > 通用最佳实践
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 部门信息表 (t_department)
-- 按部门设定工资基数，符合开题报告"工资基数部门不同则不同"要求
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_department`;
CREATE TABLE `t_department` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_name`       VARCHAR(50)     NOT NULL COMMENT '部门名称，如：销售部、技术研发部',
    `dept_code`       VARCHAR(20)     NOT NULL COMMENT '部门编码，唯一标识',
    `base_salary`     DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '该部门工资基数（元），不同部门基数不同',
    `description`     VARCHAR(255)    DEFAULT NULL COMMENT '部门职能描述',
    `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '排列顺序',
    `status`          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    UNIQUE KEY `uk_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门信息表';

-- -----------------------------------------------------------------------------
-- 2. 岗位信息表 (t_position)
-- 岗位属于部门，岗位影响薪资等级范围
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_position`;
CREATE TABLE `t_position` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID，外键关联t_department',
    `position_name`   VARCHAR(50)     NOT NULL COMMENT '岗位名称，如：后端开发工程师、UI设计师',
    `salary_min`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '该岗位薪资范围下限（元）',
    `salary_max`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '该岗位薪资范围上限（元）',
    `description`     VARCHAR(255)    DEFAULT NULL COMMENT '岗位职责描述',
    `status`          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dept_id` (`dept_id`),
    CONSTRAINT `fk_position_dept` FOREIGN KEY (`dept_id`) REFERENCES `t_department`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';

-- -----------------------------------------------------------------------------
-- 3. 系统用户表 (t_user)
-- RBAC权限基础：存储登录账户，角色区分管理员/经理/员工
-- 与员工表分离，符合第三范式（账户信息≠人事档案）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`        VARCHAR(50)     NOT NULL COMMENT '登录用户名/工号',
    `password`        VARCHAR(128)    NOT NULL COMMENT '密码（BCrypt加密存储）',
    `role`            TINYINT         NOT NULL DEFAULT 3 COMMENT '角色：1=超级管理员，2=部门经理，3=普通员工',
    `real_name`       VARCHAR(30)     NOT NULL COMMENT '真实姓名，冗余字段用于显示',
    `avatar`          VARCHAR(255)    DEFAULT NULL COMMENT '头像文件路径',
    `email`           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱地址',
    `status`          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
    `last_login_time` DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(50)     DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表（RBAC账户）';

-- -----------------------------------------------------------------------------
-- 4. 员工档案表 (t_employee)
-- 核心人事档案，包含入职时间（用于累计预扣个税基数计算）、银行卡号等
-- 基本工资保存个人实际薪资，在部门工资基数范围内由管理员设定
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_employee`;
CREATE TABLE `t_employee` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联系统用户ID（t_user）',
    `emp_no`          VARCHAR(20)     NOT NULL COMMENT '工号，业务唯一标识（如：001、2580）',
    `real_name`       VARCHAR(30)     NOT NULL COMMENT '员工姓名',
    `gender`          TINYINT         NOT NULL DEFAULT 1 COMMENT '性别：1=男，2=女',
    `id_card`         VARCHAR(18)     DEFAULT NULL COMMENT '身份证号（18位），用于社保登记',
    `phone`           VARCHAR(11)     NOT NULL COMMENT '手机号码',
    `dept_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属部门ID，外键关联t_department',
    `position_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '岗位ID，外键关联t_position',
    `manager_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '直属经理的用户ID（t_user），用于工资条审批',
    `hire_date`       DATE            NOT NULL COMMENT '入职日期，用于计算工龄及累计预扣个税的起始年份',
    `base_salary`     DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '员工个人基本工资（元），由部门工资基数决定范围',
    `bank_account`    VARCHAR(25)     DEFAULT NULL COMMENT '银行账户号（储蓄卡号，最长25位）',
    `bank_name`       VARCHAR(50)     DEFAULT NULL COMMENT '开户行名称，如：中国工商银行',
    `avatar`          VARCHAR(255)    DEFAULT NULL COMMENT '头像文件路径',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '在职状态：1=在职，2=离职，3=试用期',
    `remark`          VARCHAR(500)    DEFAULT NULL COMMENT '备注信息',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（即录入系统时间）',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_no` (`emp_no`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_manager_id` (`manager_id`),
    CONSTRAINT `fk_employee_user` FOREIGN KEY (`user_id`) REFERENCES `t_user`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_employee_dept` FOREIGN KEY (`dept_id`) REFERENCES `t_department`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_employee_position` FOREIGN KEY (`position_id`) REFERENCES `t_position`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工档案信息表';

-- -----------------------------------------------------------------------------
-- 5. 薪资项配置表 (t_salary_item)
-- 自定义薪资结构中各薪资组成项（基本工资、绩效、加班费、津贴等）
-- 分离配置与数据，支持灵活扩展
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_salary_item`;
CREATE TABLE `t_salary_item` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `item_name`       VARCHAR(50)     NOT NULL COMMENT '薪资项名称，如：基本工资、绩效奖金、加班工资、交通津贴',
    `item_code`       VARCHAR(30)     NOT NULL COMMENT '薪资项编码，如：BASE_SALARY、PERF_BONUS',
    `item_type`       TINYINT         NOT NULL DEFAULT 1 COMMENT '类型：1=加项（应发），2=减项（扣款），3=计算基础项',
    `calc_method`     TINYINT         NOT NULL DEFAULT 1 COMMENT '计算方式：1=固定金额，2=按比例，3=按公式自动计算',
    `default_value`   DECIMAL(10, 2)  DEFAULT 0.00 COMMENT '默认金额或比例系数',
    `is_taxable`      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否计入个税应税所得：1=是，0=否',
    `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '计算排列顺序',
    `is_active`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1=是，0=否',
    `description`     VARCHAR(255)    DEFAULT NULL COMMENT '薪资项说明',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资项配置表（薪资结构组成项）';

-- -----------------------------------------------------------------------------
-- 6. 薪资结构模板表 (t_salary_structure)
-- 对应前端"薪资结构"页面，定义各部门/岗位的薪资结构模板
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_salary_structure`;
CREATE TABLE `t_salary_structure` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `structure_name`  VARCHAR(100)    NOT NULL COMMENT '结构名称，如：技术岗薪资结构、销售岗薪资结构',
    `dept_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '适用部门ID（NULL表示通用），外键关联t_department',
    `base_salary_ratio` DECIMAL(5, 4) NOT NULL DEFAULT 1.0000 COMMENT '基本工资占比系数（如0.6表示60%）',
    `perf_ratio`      DECIMAL(5, 4)   NOT NULL DEFAULT 0.20 COMMENT '绩效工资占比',
    `allowance_standard` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '标准津贴（交通+餐补+通讯等固定津贴合计，元）',
    `overtime_hourly_rate` DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '加班小时费率（元/小时），为0则按法定计算',
    `description`     VARCHAR(255)    DEFAULT NULL COMMENT '结构说明',
    `is_active`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dept_id` (`dept_id`),
    CONSTRAINT `fk_structure_dept` FOREIGN KEY (`dept_id`) REFERENCES `t_department`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资结构模板配置表';

-- -----------------------------------------------------------------------------
-- 7. 考勤规则表 (t_attendance_rule)
-- 对应前端"考勤规则"页面，定义迟到/旷工/请假的扣款规则
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_attendance_rule`;
CREATE TABLE `t_attendance_rule` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_name`       VARCHAR(100)    NOT NULL COMMENT '规则名称，如：标准工作制考勤规则',
    `work_days`       INT             NOT NULL DEFAULT 22 COMMENT '月标准工作天数（用于日薪计算基数）',
    `work_hours`      DECIMAL(5, 1)   NOT NULL DEFAULT 8.0 COMMENT '每日标准工作小时数',
    `late_deduct_per_time`    DECIMAL(8, 2) NOT NULL DEFAULT 50.00 COMMENT '迟到每次扣款金额（元）',
    `late_threshold_min`      INT           NOT NULL DEFAULT 30 COMMENT '迟到认定阈值（分钟），超过此时长视为旷工半天',
    `absent_deduct_per_day`   DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '旷工每天扣款额（元），0表示按日薪全额扣除',
    `leave_deduct_ratio`      DECIMAL(5, 4) NOT NULL DEFAULT 1.0000 COMMENT '事假扣款比例，1.0=按日薪全额扣，0.5=扣半天',
    `sick_leave_deduct_ratio` DECIMAL(5, 4) NOT NULL DEFAULT 0.80 COMMENT '病假扣款比例（通常扣80%日薪）',
    `annual_leave_days`       INT           NOT NULL DEFAULT 5 COMMENT '年度带薪年假天数',
    `is_active`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否为当前生效规则：1=生效，0=历史',
    `effective_date`  DATE            NOT NULL COMMENT '规则生效日期',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤规则配置表（迟到、旷工、请假扣款规则）';

-- -----------------------------------------------------------------------------
-- 8. 月度考勤数据表 (t_attendance_record)
-- 按员工+月份记录考勤汇总结果，经理录入，作为薪资计算的核心输入
-- 对应HTML页面字段：登记编号、月份、工号、姓名、部门、
--                   出勤天数、旷工天数、迟到天数、请假天数、出勤时长
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_attendance_record`;
CREATE TABLE `t_attendance_record` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_no`       VARCHAR(20)     NOT NULL COMMENT '登记编号，系统自动生成（如：9000000007）',
    `year_month`      CHAR(7)         NOT NULL COMMENT '所属年月，格式：YYYY-MM（如：2026-01）',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '员工ID，外键关联t_employee',
    `emp_no`          VARCHAR(20)     NOT NULL COMMENT '工号（冗余，避免多表关联）',
    `emp_name`        VARCHAR(30)     NOT NULL COMMENT '员工姓名（冗余）',
    `dept_id`         BIGINT UNSIGNED NOT NULL COMMENT '部门ID（冗余）',
    `dept_name`       VARCHAR(50)     NOT NULL COMMENT '部门名称（冗余，避免多表关联）',
    `attend_days`     INT             NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
    `absent_days`     DECIMAL(4, 1)   NOT NULL DEFAULT 0 COMMENT '旷工天数（支持0.5天）',
    `late_times`      INT             NOT NULL DEFAULT 0 COMMENT '迟到次数',
    `leave_days`      DECIMAL(4, 1)   NOT NULL DEFAULT 0 COMMENT '请假天数（事假+病假合计，支持0.5天）',
    `sick_leave_days` DECIMAL(4, 1)   NOT NULL DEFAULT 0 COMMENT '其中病假天数',
    `overtime_hours`  DECIMAL(6, 1)   NOT NULL DEFAULT 0 COMMENT '加班小时数（工作日+节假日合计）',
    `attend_hours`    DECIMAL(7, 1)   NOT NULL DEFAULT 0 COMMENT '实际出勤总时长（小时）',
    `attend_deduct`   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '系统预算考勤扣款合计（元），正式计算时可能调整',
    `record_date`     DATE            NOT NULL COMMENT '登记日期',
    `manager_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '录入经理的用户ID（t_user）',
    `manager_no`      VARCHAR(20)     DEFAULT NULL COMMENT '经理账号（冗余）',
    `manager_name`    VARCHAR(30)     DEFAULT NULL COMMENT '经理姓名（冗余）',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=正常，2=已申诉调整，3=已锁定',
    `remark`          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`) COMMENT '每个员工每月只允许一条考勤记录',
    KEY `idx_year_month` (`year_month`),
    KEY `idx_emp_id` (`emp_id`),
    CONSTRAINT `fk_attend_emp` FOREIGN KEY (`emp_id`) REFERENCES `t_employee`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度考勤汇总数据表（经理按月录入）';

-- -----------------------------------------------------------------------------
-- 9. 考勤申请/异议表 (t_attendance_apply)
-- 员工申请修正考勤记录，经理审批
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_attendance_apply`;
CREATE TABLE `t_attendance_apply` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `attendance_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联的考勤记录ID（t_attendance_record）',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '申请员工ID',
    `apply_type`      TINYINT         NOT NULL DEFAULT 1 COMMENT '申请类型：1=补签，2=销假，3=加班申请，4=考勤异议',
    `apply_date`      DATE            NOT NULL COMMENT '申请针对的具体日期',
    `reason`          VARCHAR(500)    NOT NULL COMMENT '申请说明及原因',
    `proof`           VARCHAR(255)    DEFAULT NULL COMMENT '证明材料文件路径',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '审批状态：0=待审批，1=已通过，2=已拒绝',
    `review_user_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人（经理）用户ID',
    `review_comment`  VARCHAR(255)    DEFAULT NULL COMMENT '审批意见',
    `review_time`     DATETIME        DEFAULT NULL COMMENT '审批时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_emp_id` (`emp_id`),
    KEY `idx_attendance_id` (`attendance_id`),
    CONSTRAINT `fk_apply_attendance` FOREIGN KEY (`attendance_id`) REFERENCES `t_attendance_record`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤申请/异议表（员工申请，经理审批）';

-- -----------------------------------------------------------------------------
-- 10. 绩效评分表 (t_performance)
-- 经理对员工进行月度绩效评分，结果影响绩效奖金计算
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_performance`;
CREATE TABLE `t_performance` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `year_month`      CHAR(7)         NOT NULL COMMENT '考核年月，格式：YYYY-MM',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '被考核员工ID',
    `emp_no`          VARCHAR(20)     NOT NULL COMMENT '工号（冗余）',
    `emp_name`        VARCHAR(30)     NOT NULL COMMENT '员工姓名（冗余）',
    `dept_id`         BIGINT UNSIGNED NOT NULL COMMENT '部门ID（冗余）',
    `score`           DECIMAL(5, 2)   NOT NULL DEFAULT 100.00 COMMENT '绩效评分（0-120分，满分100，允许加分）',
    `grade`           CHAR(2)         DEFAULT NULL COMMENT '绩效等级：S/A/B/C/D',
    `perf_bonus_ratio` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '绩效奖金金额（元）',
    `eval_comment`    VARCHAR(500)    DEFAULT NULL COMMENT '评分说明',
    `manager_id`      BIGINT UNSIGNED NOT NULL COMMENT '评分经理用户ID',
    `manager_name`    VARCHAR(30)     NOT NULL COMMENT '评分经理姓名（冗余）',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=草稿，2=已提交，3=已确认',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`) COMMENT '每个员工每月只有一条绩效记录',
    KEY `idx_year_month` (`year_month`),
    CONSTRAINT `fk_perf_emp` FOREIGN KEY (`emp_id`) REFERENCES `t_employee`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度绩效评分表（经理评分，影响绩效奖金）';

-- -----------------------------------------------------------------------------
-- 11. 月度薪资核算表 (t_salary_record)
-- 核心薪资计算结果，对应HTML"薪资核算"页面
-- 字段：月份、工号、姓名、部门、银行卡号、基本工资、加班工资、
--       绩效奖金、其他补助（津贴）、扣款金额、实发工资、登记日期
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_salary_record`;
CREATE TABLE `t_salary_record` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `year_month`      CHAR(7)         NOT NULL COMMENT '薪资所属年月，格式：YYYY-MM（如：2026-01）',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    `emp_no`          VARCHAR(20)     NOT NULL COMMENT '工号（冗余，方便查询）',
    `emp_name`        VARCHAR(30)     NOT NULL COMMENT '员工姓名（冗余）',
    `dept_id`         BIGINT UNSIGNED NOT NULL COMMENT '部门ID（冗余）',
    `dept_name`       VARCHAR(50)     NOT NULL COMMENT '部门名称（冗余）',
    `bank_account`    VARCHAR(25)     DEFAULT NULL COMMENT '银行账户号（冗余，打工资条用）',
    `manager_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '所属经理用户ID（冗余）',
    `manager_no`      VARCHAR(20)     DEFAULT NULL COMMENT '经理账号（冗余）',
    `manager_name`    VARCHAR(30)     DEFAULT NULL COMMENT '经理姓名（冗余）',

    -- 考勤关联
    `attendance_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '关联考勤记录ID，可为NULL（人工录入时）',

    -- 应发项（加项）
    `base_salary`     DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '基本工资（元），来自员工档案',
    `overtime_pay`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '加班工资（元）',
    `perf_bonus`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '绩效奖金（元），由绩效评分系数×绩效工资基数',
    `full_attend_bonus` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '全勤奖（元），当月无迟到/旷工/请假则发放',
    `allowance`       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '津贴/补助合计（交通费+餐补+通讯费等，元）',
    `other_income`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '其他应发收入（元），如年终奖分摊等',
    `gross_salary`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '应发工资合计（元）= 以上加项之和',

    -- 扣款项（减项）
    `social_security_emp` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '个人缴纳社保费用（元），养老+医疗+失业+工伤，含公积金个人部分',
    `attend_deduct`   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '考勤扣款（迟到+旷工+请假扣款合计，元）',
    `other_deduct`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '其他扣款（元），如罚款、借款还款等',
    `income_tax`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '个人所得税（元），按累计预扣法计算',
    `total_deduct`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '扣款合计（元）= 社保+考勤扣+其他扣+个税',

    -- 实发
    `net_salary`      DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '实发工资（元）= 应发 - 扣款合计',

    -- 状态流转
    `calc_status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '计算状态：1=草稿，2=待审核，3=已审核，4=已发放，5=已驳回',
    `record_date`     DATE            NOT NULL COMMENT '核算登记日期',
    `pay_date`        DATE            DEFAULT NULL COMMENT '实际薪资发放日期',
    `issue_file`      VARCHAR(255)    DEFAULT NULL COMMENT '发放文件/回单附件路径',
    `slip_published`  TINYINT         NOT NULL DEFAULT 0 COMMENT '工资条是否已向员工/经理发布：0=未发布，1=已发布',
    `slip_publish_time` DATETIME      DEFAULT NULL COMMENT '工资条发布时间',
    `remark`          VARCHAR(500)    DEFAULT NULL COMMENT '备注说明',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`) COMMENT '每个员工每月只有一条薪资记录',
    KEY `idx_year_month` (`year_month`),
    KEY `idx_emp_id` (`emp_id`),
    KEY `idx_calc_status` (`calc_status`),
    CONSTRAINT `fk_salary_emp` FOREIGN KEY (`emp_id`) REFERENCES `t_employee`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_salary_attendance` FOREIGN KEY (`attendance_id`) REFERENCES `t_attendance_record`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度薪资核算记录表（应发/扣款/实发明细）';

-- -----------------------------------------------------------------------------
-- 12. 个税及社保累计表 (t_tax_accumulate)
-- 支持跨月累计预扣预缴法（IIT累进税率）
-- 每年1月重置，全年累计计算，确保个税计算正确
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_tax_accumulate`;
CREATE TABLE `t_tax_accumulate` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    `tax_year`        CHAR(4)         NOT NULL COMMENT '税务年度（如：2026），每年独立累计',
    `year_month`      CHAR(7)         NOT NULL COMMENT '截至当月，格式：YYYY-MM',

    -- 本月数据
    `month_taxable_income`   DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '本月应纳税所得额（元）= 应发工资 - 个人五险一金 - 5000起征点（专项附加扣除当前按0处理）',
    `month_tax`              DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本月预缴个税（元）',
    `month_social_security`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本月个人缴纳社保（不含公积金，元）',
    `month_fund`             DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本月个人公积金（元）',
    `month_special_deduct`   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本月专项附加扣除（字段保留，当前业务按0处理，元）',

    -- 累计数据（用于下月计算）
    `accum_taxable_income`   DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '年度累计应纳税所得额（元）',
    `accum_tax`              DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '年度累计已预缴个税（元）',
    `accum_gross`            DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '年度累计应发工资总额（元）',
    `accum_social_security`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '年度累计个人社保（元）',
    `accum_special_deduct`   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '年度累计专项附加扣除（字段保留，当前业务按0处理，元）',

    `salary_record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联薪资记录ID（t_salary_record）',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`) COMMENT '每个员工每月一条累计记录',
    KEY `idx_emp_tax_year` (`emp_id`, `tax_year`),
    CONSTRAINT `fk_tax_emp` FOREIGN KEY (`emp_id`) REFERENCES `t_employee`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个税及社保年度累计预扣表（跨月累计法）';

-- -----------------------------------------------------------------------------
-- 13. 社保公积金配置表 (t_social_security_config)
-- 存储各险种缴纳比例，管理员可配置，用于自动计算个人和单位缴费
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_social_security_config`;
CREATE TABLE `t_social_security_config` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_name`     VARCHAR(50)     NOT NULL COMMENT '配置方案名称，如：2026年社保方案',
    `pension_emp_ratio`    DECIMAL(5, 4) NOT NULL DEFAULT 0.0800 COMMENT '养老保险个人缴纳比例（8%）',
    `pension_comp_ratio`   DECIMAL(5, 4) NOT NULL DEFAULT 0.1600 COMMENT '养老保险单位缴纳比例（16%）',
    `medical_emp_ratio`    DECIMAL(5, 4) NOT NULL DEFAULT 0.0200 COMMENT '医疗保险个人缴纳比例（2%）',
    `medical_comp_ratio`   DECIMAL(5, 4) NOT NULL DEFAULT 0.0800 COMMENT '医疗保险单位缴纳比例（8%）',
    `unemployment_emp_ratio` DECIMAL(5, 4) NOT NULL DEFAULT 0.0030 COMMENT '失业保险个人缴纳比例（0.3%）',
    `unemployment_comp_ratio` DECIMAL(5, 4) NOT NULL DEFAULT 0.0050 COMMENT '失业保险单位缴纳比例（展示备用）',
    `injury_comp_ratio`    DECIMAL(5, 4) NOT NULL DEFAULT 0.0050 COMMENT '工伤保险单位缴纳比例（个人不缴）',
    `fund_emp_ratio`       DECIMAL(5, 4) NOT NULL DEFAULT 0.1200 COMMENT '住房公积金个人缴纳比例（12%）',
    `fund_comp_ratio`      DECIMAL(5, 4) NOT NULL DEFAULT 0.1200 COMMENT '住房公积金单位缴纳比例',
    `calc_base_min`   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '缴费基数下限（元）',
    `calc_base_max`   DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '缴费基数上限（元）',
    `is_active`       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否为当前生效方案',
    `effective_date`  DATE            NOT NULL COMMENT '生效日期',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社保及公积金缴纳比例配置表';

-- -----------------------------------------------------------------------------
-- 14. 薪资发放记录表 (t_salary_payment)
-- 对应前端"薪资发放"页面，记录每次实际发放操作
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_salary_payment`;
CREATE TABLE `t_salary_payment` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `salary_record_id` BIGINT UNSIGNED NOT NULL COMMENT '关联薪资核算记录ID（t_salary_record）',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '员工ID（冗余）',
    `emp_no`          VARCHAR(20)     NOT NULL COMMENT '工号（冗余）',
    `emp_name`        VARCHAR(30)     NOT NULL COMMENT '员工姓名（冗余）',
    `year_month`      CHAR(7)         NOT NULL COMMENT '薪资所属年月（冗余）',
    `net_salary`      DECIMAL(10, 2)  NOT NULL COMMENT '实发金额（元）',
    `bank_account`    VARCHAR(25)     NOT NULL COMMENT '收款银行账户',
    `bank_name`       VARCHAR(50)     DEFAULT NULL COMMENT '收款开户行',
    `pay_date`        DATE            NOT NULL COMMENT '发放日期',
    `pay_method`      TINYINT         NOT NULL DEFAULT 1 COMMENT '发放方式：1=银行转账，2=现金，3=支票',
    `pay_status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '发放状态：1=待发放，2=已发放，3=发放失败',
    `operator_id`     BIGINT UNSIGNED NOT NULL COMMENT '操作人（管理员/经理）用户ID',
    `operator_name`   VARCHAR(30)     NOT NULL COMMENT '操作人姓名（冗余）',
    `remark`          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_salary_record` (`salary_record_id`) COMMENT '一条薪资记录只对应一条发放记录',
    KEY `idx_emp_id` (`emp_id`),
    KEY `idx_year_month` (`year_month`),
    CONSTRAINT `fk_payment_salary` FOREIGN KEY (`salary_record_id`) REFERENCES `t_salary_record`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资发放记录表（工资实际打款记录）';

-- -----------------------------------------------------------------------------
-- 15. 薪资反馈/投诉表 (t_salary_feedback)
-- 员工对工资条有异议可提交反馈，对应前端"薪资反馈"和"投诉反馈"页面
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_salary_feedback`;
CREATE TABLE `t_salary_feedback` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `feedback_type`   TINYINT         NOT NULL DEFAULT 1 COMMENT '类型：1=薪资异议，2=计算错误投诉，3=其他投诉',
    `emp_id`          BIGINT UNSIGNED NOT NULL COMMENT '提交员工ID',
    `salary_record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联薪资记录ID（可以是针对某月工资的反馈）',
    `year_month`      CHAR(7)         DEFAULT NULL COMMENT '反馈针对的薪资年月',
    `title`           VARCHAR(100)    NOT NULL COMMENT '反馈标题',
    `content`         VARCHAR(2000)   NOT NULL COMMENT '详细反馈内容',
    `attachment`      VARCHAR(255)    DEFAULT NULL COMMENT '附件路径',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '处理状态：0=待处理，1=处理中，2=已解决，3=已驳回',
    `reply_content`   VARCHAR(1000)   DEFAULT NULL COMMENT '管理员/经理回复内容',
    `reply_user_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '回复人用户ID',
    `reply_time`      DATETIME        DEFAULT NULL COMMENT '回复时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_emp_id` (`emp_id`),
    KEY `idx_salary_record_id` (`salary_record_id`),
    CONSTRAINT `fk_feedback_emp` FOREIGN KEY (`emp_id`) REFERENCES `t_employee`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资反馈与投诉记录表';

-- -----------------------------------------------------------------------------
-- 16. 异常上报表 (t_anomaly_report)
-- 经理上报薪资或考勤异常，对应前端"异常上报"页面
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_anomaly_report`;
CREATE TABLE `t_anomaly_report` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_type`     TINYINT         NOT NULL DEFAULT 1 COMMENT '异常类型：1=考勤异常，2=薪资计算异常，3=系统数据异常',
    `reporter_id`     BIGINT UNSIGNED NOT NULL COMMENT '上报人（经理）用户ID',
    `emp_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '涉及员工ID（可为NULL，如系统级异常）',
    `year_month`      CHAR(7)         DEFAULT NULL COMMENT '涉及年月',
    `title`           VARCHAR(100)    NOT NULL COMMENT '异常标题',
    `description`     VARCHAR(2000)   NOT NULL COMMENT '异常详细描述',
    `impact`          VARCHAR(500)    DEFAULT NULL COMMENT '影响范围说明',
    `attachment`      VARCHAR(255)    DEFAULT NULL COMMENT '附件路径',
    `severity`        TINYINT         NOT NULL DEFAULT 2 COMMENT '严重程度：1=严重，2=一般，3=轻微',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '处理状态：0=待处理，1=处理中，2=已解决',
    `handler_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人（管理员）用户ID',
    `handle_result`   VARCHAR(500)    DEFAULT NULL COMMENT '处理结果说明',
    `handle_time`     DATETIME        DEFAULT NULL COMMENT '处理完成时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    PRIMARY KEY (`id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_emp_id` (`emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常上报记录表（经理上报）';

-- -----------------------------------------------------------------------------
-- 17. 通知公告表 (t_announcement)
-- 管理员发布系统公告，员工前台可查看
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_announcement`;
CREATE TABLE `t_announcement` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`           VARCHAR(200)    NOT NULL COMMENT '公告标题',
    `content`         MEDIUMTEXT      NOT NULL COMMENT '公告内容（富文本）',
    `cover_image`     VARCHAR(255)    DEFAULT NULL COMMENT '封面图片路径',
    `pub_user_id`     BIGINT UNSIGNED NOT NULL COMMENT '发布人用户ID',
    `pub_user_name`   VARCHAR(30)     NOT NULL COMMENT '发布人姓名（冗余）',
    `target_role`     TINYINT         NOT NULL DEFAULT 0 COMMENT '目标受众：0=全部，1=管理员，2=经理，3=员工',
    `is_top`          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否置顶：1=是，0=否',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=已发布，0=草稿，2=已撤回',
    `pub_time`        DATETIME        DEFAULT NULL COMMENT '发布时间',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_pub_time` (`pub_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- -----------------------------------------------------------------------------
-- 18. 系统操作日志表 (t_sys_log)
-- 记录关键操作日志，用于系统审计
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `t_sys_log`;
CREATE TABLE `t_sys_log` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID（NULL表示系统自动操作）',
    `username`        VARCHAR(50)     DEFAULT NULL COMMENT '操作用户名（冗余）',
    `module`          VARCHAR(50)     NOT NULL COMMENT '操作模块，如：薪资管理、员工管理',
    `action`          VARCHAR(100)    NOT NULL COMMENT '操作描述，如：新增员工、发放薪资',
    `method`          VARCHAR(200)    DEFAULT NULL COMMENT '请求方法路径（Controller方法）',
    `params`          TEXT            DEFAULT NULL COMMENT '请求参数（JSON格式，敏感字段脱敏）',
    `result`          TINYINT         NOT NULL DEFAULT 1 COMMENT '操作结果：1=成功，0=失败',
    `error_msg`       VARCHAR(500)    DEFAULT NULL COMMENT '失败错误信息',
    `ip`              VARCHAR(50)     DEFAULT NULL COMMENT '操作IP地址',
    `cost_time`       INT             DEFAULT NULL COMMENT '响应耗时（毫秒）',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作审计日志表';

-- -----------------------------------------------------------------------------
-- 初始化数据：管理员账户（密码：admin123，BCrypt加密后替换）
-- -----------------------------------------------------------------------------
INSERT INTO `t_user` (`username`, `password`, `role`, `real_name`, `status`)
VALUES ('admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', 1, '系统管理员', 1);

SET FOREIGN_KEY_CHECKS = 1;
