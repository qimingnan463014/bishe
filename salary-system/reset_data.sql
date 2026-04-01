-- =============================================================================
-- 基于Java的员工薪资管理系统 - 数据库全量重建脚本 (V11.0 终极兼容版)
-- 修复：
-- 1. 补全 t_department 缺失的 `description`, `sort_order`, `update_time` 等字段 (解决保存报错)
-- 2. 补全 t_employee 缺失的 `id_card`, `position_id`, `bank_name`, `remark` (解决查询空白)
-- 3. 全量 18 张表必须严格遵循 db_schema.sql，并包裹反引号 (`)
-- 4. 优化 5名经理 + 10名员工数据，并将角色信息回馈到 t_user
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 全量清理
DROP TABLE IF EXISTS `t_sys_log`;
DROP TABLE IF EXISTS `t_announcement`;
DROP TABLE IF EXISTS `t_anomaly_report`;
DROP TABLE IF EXISTS `t_salary_feedback`;
DROP TABLE IF EXISTS `t_salary_payment`;
DROP TABLE IF EXISTS `t_social_security_config`;
DROP TABLE IF EXISTS `t_tax_accumulate`;
DROP TABLE IF EXISTS `t_salary_record`;
DROP TABLE IF EXISTS `t_performance`;
DROP TABLE IF EXISTS `t_attendance_apply`;
DROP TABLE IF EXISTS `t_attendance_record`;
DROP TABLE IF EXISTS `t_attendance_rule`;
DROP TABLE IF EXISTS `t_salary_structure`;
DROP TABLE IF EXISTS `t_salary_item`;
DROP TABLE IF EXISTS `t_employee`;
DROP TABLE IF EXISTS `t_user`;
DROP TABLE IF EXISTS `t_position`;
DROP TABLE IF EXISTS `t_department`;

-- 2. 表结构定义

CREATE TABLE `t_department` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dept_name` VARCHAR(50) NOT NULL,
  `dept_code` VARCHAR(20) NOT NULL,
  `base_salary` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `description` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT '0',
  `manager_id` BIGINT UNSIGNED DEFAULT NULL,
  `status` TINYINT(1) NOT NULL DEFAULT '1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  UNIQUE KEY `uk_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `role` TINYINT NOT NULL DEFAULT '3',
  `real_name` VARCHAR(30) NOT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `status` TINYINT(1) NOT NULL DEFAULT '1',
  `last_login_time` DATETIME DEFAULT NULL,
  `last_login_ip` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_position` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `position_name` VARCHAR(50) NOT NULL,
  `salary_min` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `salary_max` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `description` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT(1) NOT NULL DEFAULT '1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_employee` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `emp_no` VARCHAR(20) NOT NULL,
  `real_name` VARCHAR(30) NOT NULL,
  `gender` TINYINT NOT NULL DEFAULT '1',
  `id_card` VARCHAR(18) DEFAULT NULL,
  `phone` VARCHAR(11) NOT NULL,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `position_id` BIGINT UNSIGNED DEFAULT NULL,
  `manager_id` BIGINT UNSIGNED DEFAULT NULL,
  `hire_date` DATE NOT NULL,
  `base_salary` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `bank_account` VARCHAR(25) DEFAULT NULL,
  `bank_name` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `remark` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_emp_no` (`emp_no`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 补全其余所有表结构（同步 db_schema.sql 核心列）
CREATE TABLE `t_salary_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `item_name` VARCHAR(50) NOT NULL,
  `item_code` VARCHAR(30) NOT NULL,
  `item_type` TINYINT NOT NULL DEFAULT '1',
  `calc_method` TINYINT NOT NULL DEFAULT '1',
  `default_value` DECIMAL(10,2) DEFAULT '0.00',
  `is_taxable` TINYINT(1) NOT NULL DEFAULT '1',
  `sort_order` INT NOT NULL DEFAULT '0',
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  `description` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_salary_structure` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `structure_name` VARCHAR(100) NOT NULL,
  `dept_id` BIGINT UNSIGNED DEFAULT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_attendance_rule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `rule_name` VARCHAR(100) NOT NULL,
  `work_days` INT NOT NULL DEFAULT '22',
  `work_hours` DECIMAL(5,1) NOT NULL DEFAULT '8.0',
  `is_active` TINYINT(1) NOT NULL DEFAULT '1',
  `effective_date` DATE NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_attendance_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `record_no` VARCHAR(20) NOT NULL,
  `year_month` CHAR(7) NOT NULL,
  `emp_id` BIGINT UNSIGNED NOT NULL,
  `emp_no` VARCHAR(20) NOT NULL,
  `emp_name` VARCHAR(30) NOT NULL,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `dept_name` VARCHAR(50) NOT NULL,
  `attend_days` INT NOT NULL DEFAULT '0',
  `absent_days` DECIMAL(5,2) DEFAULT '0.00',
  `late_times` INT DEFAULT '0',
  `leave_days` DECIMAL(5,2) DEFAULT '0.00',
  `sick_leave_days` DECIMAL(5,2) DEFAULT '0.00',
  `overtime_hours` DECIMAL(5,2) DEFAULT '0.00',
  `attend_hours` DECIMAL(5,2) DEFAULT '0.00',
  `attend_deduct` DECIMAL(10,2) DEFAULT '0.00',
  `record_date` DATE NOT NULL,
  `manager_id` BIGINT UNSIGNED DEFAULT NULL,
  `manager_no` VARCHAR(20) DEFAULT NULL,
  `manager_name` VARCHAR(30) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `remark` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_salary_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `year_month` CHAR(7) NOT NULL,
  `emp_id` BIGINT UNSIGNED NOT NULL,
  `emp_no` VARCHAR(20) NOT NULL,
  `emp_name` VARCHAR(30) NOT NULL,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `dept_name` VARCHAR(50) NOT NULL,
  `bank_account` VARCHAR(25) DEFAULT NULL,
  `manager_id` BIGINT UNSIGNED DEFAULT NULL,
  `manager_no` VARCHAR(20) DEFAULT NULL,
  `manager_name` VARCHAR(30) DEFAULT NULL,
  `attendance_id` BIGINT UNSIGNED DEFAULT NULL,
  `base_salary` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `overtime_pay` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `perf_bonus` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `full_attend_bonus` DECIMAL(10,2) DEFAULT '0.00',
  `allowance` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `other_income` DECIMAL(10,2) DEFAULT '0.00',
  `gross_salary` DECIMAL(10,2) DEFAULT '0.00',
  `social_security_emp` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `attend_deduct` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `other_deduct` DECIMAL(10,2) DEFAULT '0.00',
  `income_tax` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `total_deduct` DECIMAL(10,2) DEFAULT '0.00',
  `net_salary` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `calc_status` TINYINT NOT NULL DEFAULT '1',
  `record_date` DATE NOT NULL,
  `pay_date` DATE DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_performance` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `year_month` CHAR(7) NOT NULL,
  `emp_id` BIGINT UNSIGNED NOT NULL,
  `emp_no` VARCHAR(20) DEFAULT NULL,
  `emp_name` VARCHAR(30) DEFAULT NULL,
  `dept_id` BIGINT UNSIGNED DEFAULT NULL,
  `work_attitude` DECIMAL(5,2) DEFAULT NULL,
  `business_skill` DECIMAL(5,2) DEFAULT NULL,
  `work_performance` DECIMAL(5,2) DEFAULT NULL,
  `bonus_deduct` DECIMAL(5,2) DEFAULT '0.00',
  `score` DECIMAL(5,2) DEFAULT NULL,
  `grade` VARCHAR(2) DEFAULT NULL,
  `perf_bonus_ratio` DECIMAL(5,4) DEFAULT '1.0000',
  `eval_comment` VARCHAR(500) DEFAULT NULL,
  `manager_id` BIGINT UNSIGNED DEFAULT NULL,
  `manager_name` VARCHAR(30) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `t_tax_accumulate` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `emp_id` BIGINT,
  `tax_year` VARCHAR(4) DEFAULT NULL,
  `year_month` CHAR(7),
  `month_taxable_income` DECIMAL(12,2) DEFAULT '0.00',
  `month_tax` DECIMAL(12,2) DEFAULT '0.00',
  `month_social_security` DECIMAL(12,2) DEFAULT '0.00',
  `month_fund` DECIMAL(12,2) DEFAULT '0.00',
  `month_special_deduct` DECIMAL(12,2) DEFAULT '0.00',
  `accum_taxable_income` DECIMAL(12,2) DEFAULT '0.00',
  `accum_tax` DECIMAL(12,2) DEFAULT '0.00',
  `accum_gross` DECIMAL(12,2) DEFAULT '0.00',
  `accum_social_security` DECIMAL(12,2) DEFAULT '0.00',
  `accum_special_deduct` DECIMAL(12,2) DEFAULT '0.00',
  `salary_record_id` BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_social_security_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `config_name` VARCHAR(100) DEFAULT NULL,
  `pension_rate` DECIMAL(5,4) DEFAULT '0.1600',
  `medical_rate` DECIMAL(5,4) DEFAULT '0.0800',
  `unemployment_rate` DECIMAL(5,4) DEFAULT '0.0050',
  `injury_rate` DECIMAL(5,4) DEFAULT '0.0050',
  `maternity_rate` DECIMAL(5,4) DEFAULT '0.0000',
  `fund_rate` DECIMAL(5,4) DEFAULT '0.1200',
  `base_min` DECIMAL(10,2) DEFAULT '3000.00',
  `base_max` DECIMAL(10,2) DEFAULT '30000.00',
  `effective_date` DATE DEFAULT NULL,
  `is_active` TINYINT(1) DEFAULT '1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_attendance_apply` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `attendance_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_no` VARCHAR(20) DEFAULT NULL,
  `emp_name` VARCHAR(30) DEFAULT NULL,
  `dept_id` BIGINT UNSIGNED DEFAULT NULL,
  `apply_type` TINYINT DEFAULT NULL,
  `apply_date` DATE DEFAULT NULL,
  `reason` VARCHAR(500) DEFAULT NULL,
  `proof` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT '0',
  `review_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `review_user_name` VARCHAR(30) DEFAULT NULL,
  `review_comment` VARCHAR(500) DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_salary_payment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `salary_record_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_no` VARCHAR(20) DEFAULT NULL,
  `emp_name` VARCHAR(30) DEFAULT NULL,
  `year_month` CHAR(7) DEFAULT NULL,
  `net_salary` DECIMAL(10,2) DEFAULT '0.00',
  `bank_account` VARCHAR(25) DEFAULT NULL,
  `bank_name` VARCHAR(50) DEFAULT NULL,
  `pay_date` DATE DEFAULT NULL,
  `pay_method` TINYINT DEFAULT '1',
  `pay_status` TINYINT DEFAULT '1',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL,
  `operator_name` VARCHAR(30) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_salary_feedback` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `feedback_type` TINYINT DEFAULT NULL,
  `emp_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_no` VARCHAR(20) DEFAULT NULL,
  `emp_name` VARCHAR(30) DEFAULT NULL,
  `salary_record_id` BIGINT UNSIGNED DEFAULT NULL,
  `year_month` CHAR(7) DEFAULT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `content` TEXT DEFAULT NULL,
  `attachment` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT DEFAULT '0',
  `reply_content` TEXT DEFAULT NULL,
  `reply_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `reply_user_name` VARCHAR(30) DEFAULT NULL,
  `reply_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `t_anomaly_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `report_type` TINYINT DEFAULT NULL,
  `reporter_id` BIGINT UNSIGNED DEFAULT NULL,
  `reporter_name` VARCHAR(30) DEFAULT NULL,
  `emp_id` BIGINT UNSIGNED DEFAULT NULL,
  `emp_no` VARCHAR(20) DEFAULT NULL,
  `emp_name` VARCHAR(30) DEFAULT NULL,
  `year_month` CHAR(7) DEFAULT NULL,
  `title` VARCHAR(200) DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  `status` TINYINT DEFAULT '0',
  `process_result` TEXT DEFAULT NULL,
  `processor_id` BIGINT UNSIGNED DEFAULT NULL,
  `processor_name` VARCHAR(30) DEFAULT NULL,
  `process_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `t_announcement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL,
  `content` MEDIUMTEXT NOT NULL,
  `cover_image` VARCHAR(255) DEFAULT NULL,
  `pub_user_id` BIGINT UNSIGNED NOT NULL,
  `pub_user_name` VARCHAR(30) NOT NULL,
  `target_role` TINYINT NOT NULL DEFAULT '0',
  `is_top` TINYINT(1) NOT NULL DEFAULT '0',
  `status` TINYINT NOT NULL DEFAULT '1',
  `pub_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `t_sys_log` (`id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, `username` VARCHAR(50), `module` VARCHAR(50), `action` VARCHAR(100), `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;

-- 3. 初始化数据

-- 部门
INSERT INTO `t_department` (`id`, `dept_name`, `dept_code`, `base_salary`, `description`, `manager_id`) VALUES 
(1, '技术研发部', 'TECH', 8000.00, '负责系统开发与维护', 101),
(2, '市场销售部', 'SALE', 5000.00, '负责产品推广与销售', 102),
(3, '人力资源部', 'HR', 6000.00, '负责人才招聘与行政', 103),
(4, '财务管理部', 'FIN', 7000.00, '负责资金核算与税务', 104);

-- 用户 (5经理 + 10员工)
INSERT INTO `t_user` (`id`, `username`, `password`, `role`, `real_name`, `status`) VALUES 
(1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOAK7os8laRKLS7G9xz8eXInD18.', 1, '系统管理员', 1),
(101, '10001', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 2, '张伟', 1),
(102, '10002', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 2, '李明', 1),
(103, '10003', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 2, '王强', 1),
(104, '10004', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 2, '赵磊', 1),
(105, '10005', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 2, '刘洋', 1),
(201, '001', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '王建国', 1),
(202, '002', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '林晓燕', 1),
(203, '003', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '陈志远', 1),
(204, '004', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '李雪梅', 1),
(205, '005', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '孙浩然', 1),
(206, '006', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '周婷婷', 1),
(207, '007', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '吴俊杰', 1),
(208, '008', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '郑美玲', 1),
(209, '009', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '苏文博', 1),
(210, '010', '$2a$10$76P/HIsOOCFfG.Zonm/14O.SjS2KozI.O96S6qM9.0Xq6W76.O6uC', 3, '林雨萱', 1);

-- 档案
INSERT INTO `t_employee` (`id`, `user_id`, `emp_no`, `real_name`, `gender`, `id_card`, `phone`, `dept_id`, `position_id`, `manager_id`, `hire_date`, `base_salary`, `bank_account`, `bank_name`, `status`, `remark`) VALUES 
(1, 101, '10001', '张伟', 1, '350102199001011234', '13800010001', 1, 1, NULL, '2020-01-01', 15000.00, '6222000000000010001', '中国工商银行', 1, '技术部经理'),
(2, 102, '10002', '李明', 1, '350103199202022345', '13800010002', 2, 5, NULL, '2020-02-01', 12000.00, '6222000000000010002', '中国建设银行', 1, '销售部经理'),
(3, 103, '10003', '王强', 1, '350104199303033456', '13800010003', 3, 8, NULL, '2020-03-01', 11000.00, '6222000000000010003', '中国农业银行', 1, '人事部经理'),
(4, 104, '10004', '赵磊', 1, '350105199404044567', '13800010004', 4, 10, NULL, '2020-04-01', 13000.00, '6222000000000010004', '中国银行', 1, '财务部经理'),
(5, 105, '10005', '刘洋', 1, '350111199505055678', '13800010005', 1, 2, 101, '2020-05-01', 14000.00, '6222000000000010005', '中国工商银行', 1, '高级技术经理'),
(6, 201, '001', '王建国', 1, '350121199601156789', '13900000001', 1, 4, 101, '2023-01-15', 10000.00, '6222000000000000001', '中国工商银行', 1, '初级工程师'),
(7, 202, '002', '林晓燕', 2, '350122199702206890', '13900000002', 1, 3, 101, '2023-02-20', 8500.00, '6222000000000000002', '中国建设银行', 1, '中级工程师'),
(8, 203, '003', '陈志远', 1, '350123199803107901', '13900000003', 2, 7, 102, '2023-03-10', 6000.00, '6222000000000000003', '中国农业银行', 1, '销售专员'),
(9, 204, '004', '李雪梅', 2, '350124199904158012', '13900000004', 2, 7, 102, '2023-04-01', 5500.00, '6222000000000000004', '中国银行', 1, '销售助理'),
(10, 205, '005', '孙浩然', 1, '350125199505209123', '13900000005', 3, 9, 103, '2023-05-15', 7000.00, '6222000000000000005', '中国工商银行', 1, '人事专员'),
(11, 206, '006', '周婷婷', 2, '350126199606258234', '13900000006', 3, 9, 103, '2023-06-20', 7500.00, '6222000000000000006', '中国建设银行', 1, '人事主管'),
(12, 207, '007', '吴俊杰', 1, '350127199707307345', '13900000007', 4, 11, 104, '2023-07-10', 8000.00, '6222000000000000007', '中国农业银行', 1, '会计师'),
(13, 208, '008', '郑美玲', 2, '350128199808059456', '13900000008', 4, 12, 104, '2023-08-01', 8200.00, '6222000000000000008', '中国银行', 1, '出纳'),
(14, 209, '009', '苏文博', 1, '350129199909105678', '13900000010', 1, 2, 101, '2023-09-15', 9000.00, '6222000000000000009', '中国工商银行', 1, '高级研发'),
(15, 210, '010', '林雨萱', 2, '350181200010206789', '13900110001', 2, 7, 102, '2023-10-20', 6500.00, '6222000000000000010', '中国建设银行', 1, '销售顾问');

-- 岗位数据
INSERT INTO `t_position` (`dept_id`, `position_name`, `salary_min`, `salary_max`, `description`, `status`) VALUES 
(1, '技术总监', 20000.00, 35000.00, '负责技术团队管理和技术架构设计', 1),
(1, '高级工程师', 12000.00, 20000.00, '负责核心功能开发和技术难题解决', 1),
(1, '中级工程师', 8000.00, 15000.00, '负责功能模块开发和维护', 1),
(1, '初级工程师', 5000.00, 10000.00, '协助完成开发任务和学习成长', 1),
(2, '销售总监', 18000.00, 30000.00, '负责销售团队管理和销售策略制定', 1),
(2, '销售经理', 10000.00, 18000.00, '负责区域销售和客户关系维护', 1),
(2, '销售专员', 5000.00, 12000.00, '负责客户开发和销售执行', 1),
(3, '人事总监', 15000.00, 25000.00, '负责人力资源战略规划和团队管理', 1),
(3, '人事专员', 6000.00, 12000.00, '负责招聘、培训和员工关系管理', 1),
(4, '财务总监', 20000.00, 35000.00, '负责财务管理和资金运作', 1),
(4, '会计', 7000.00, 14000.00, '负责财务核算和报表编制', 1),
(4, '出纳', 5000.00, 10000.00, '负责现金管理和银行结算', 1);

-- 薪资项目数据
INSERT INTO `t_salary_item` (`item_name`, `item_code`, `item_type`, `calc_method`, `default_value`, `is_taxable`, `sort_order`, `is_active`, `description`) VALUES 
('基本工资', 'BASE_SALARY', 1, 1, 0.00, 1, 1, 1, '员工基本工资'),
('绩效奖金', 'PERF_BONUS', 1, 1, 0.00, 1, 2, 1, '根据绩效考核发放的奖金'),
('加班费', 'OVERTIME_PAY', 1, 1, 0.00, 1, 3, 1, '加班工资'),
('交通补贴', 'TRAFFIC_ALLOWANCE', 1, 2, 500.00, 1, 4, 1, '每月交通补贴'),
('餐补', 'MEAL_ALLOWANCE', 1, 2, 300.00, 1, 5, 1, '每月餐补'),
('住房补贴', 'HOUSING_ALLOWANCE', 1, 2, 800.00, 1, 6, 1, '每月住房补贴'),
('社会保险-个人', 'SOCIAL_SECURITY_EMP', 2, 1, 0.00, 0, 7, 1, '员工个人缴纳的社会保险'),
('住房公积金-个人', 'HOUSING_FUND_EMP', 2, 1, 0.00, 0, 8, 1, '员工个人缴纳的住房公积金'),
('个人所得税', 'INCOME_TAX', 2, 1, 0.00, 0, 9, 1, '代扣代缴的个人所得税'),
('考勤扣款', 'ATTENDANCE_DEDUCT', 2, 1, 0.00, 0, 10, 1, '迟到、早退、缺勤等扣款');

-- 薪资结构数据
INSERT INTO `t_salary_structure` (`structure_name`, `dept_id`, `is_active`) VALUES 
('技术研发部薪资结构', 1, 1),
('市场销售部薪资结构', 2, 1),
('人力资源部薪资结构', 3, 1),
('财务管理部薪资结构', 4, 1),
('通用薪资结构', NULL, 1);

-- 考勤规则数据
INSERT INTO `t_attendance_rule` (`rule_name`, `work_days`, `work_hours`, `is_active`, `effective_date`) VALUES 
('标准考勤规则', 22, 8.0, 1, '2024-01-01'),
('灵活考勤规则', 22, 8.0, 0, '2024-01-01');

-- 考勤记录数据 (2025年1月)
INSERT INTO `t_attendance_record` (`record_no`, `year_month`, `emp_id`, `emp_no`, `emp_name`, `dept_id`, `dept_name`, `attend_days`, `absent_days`, `late_times`, `leave_days`, `overtime_hours`, `attend_hours`, `attend_deduct`, `record_date`, `manager_id`, `manager_no`, `manager_name`, `status`) VALUES 
('AR202501001', '2025-01', 1, '10001', '张伟', 1, '技术研发部', 22, 0.00, 0, 0.00, 8.00, 176.00, 0.00, '2025-01-31', 101, '10001', '张伟', 1),
('AR202501002', '2025-01', 2, '10002', '李明', 2, '市场销售部', 21, 0.00, 1, 0.00, 4.00, 168.00, 50.00, '2025-01-31', 102, '10002', '李明', 1),
('AR202501003', '2025-01', 3, '10003', '王强', 3, '人力资源部', 22, 0.00, 0, 0.00, 2.00, 176.00, 0.00, '2025-01-31', 103, '10003', '王强', 1),
('AR202501004', '2025-01', 4, '10004', '赵磊', 4, '财务管理部', 20, 0.00, 2, 1.00, 0.00, 160.00, 200.00, '2025-01-31', 104, '10004', '赵磊', 1),
('AR202501005', '2025-01', 5, '10005', '刘洋', 1, '技术研发部', 22, 0.00, 0, 0.00, 12.00, 176.00, 0.00, '2025-01-31', 101, '10001', '张伟', 1),
('AR202501006', '2025-01', 6, '001', '王建国', 1, '技术研发部', 21, 0.00, 1, 0.00, 6.00, 168.00, 50.00, '2025-01-31', 101, '10001', '张伟', 1),
('AR202501007', '2025-01', 7, '002', '林晓燕', 1, '技术研发部', 22, 0.00, 0, 0.00, 4.00, 176.00, 0.00, '2025-01-31', 101, '10001', '张伟', 1),
('AR202501008', '2025-01', 8, '003', '陈志远', 2, '市场销售部', 22, 0.00, 0, 0.00, 2.00, 176.00, 0.00, '2025-01-31', 102, '10002', '李明', 1),
('AR202501009', '2025-01', 9, '004', '李雪梅', 2, '市场销售部', 20, 0.00, 2, 1.00, 0.00, 160.00, 200.00, '2025-01-31', 102, '10002', '李明', 1),
('AR202501010', '2025-01', 10, '005', '孙浩然', 3, '人力资源部', 22, 0.00, 0, 0.00, 2.00, 176.00, 0.00, '2025-01-31', 103, '10003', '王强', 1),
('AR202501011', '2025-01', 11, '006', '周婷婷', 3, '人力资源部', 22, 0.00, 0, 0.00, 4.00, 176.00, 0.00, '2025-01-31', 103, '10003', '王强', 1),
('AR202501012', '2025-01', 12, '007', '吴俊杰', 4, '财务管理部', 21, 0.00, 1, 0.00, 2.00, 168.00, 50.00, '2025-01-31', 104, '10004', '赵磊', 1),
('AR202501013', '2025-01', 13, '008', '郑美玲', 4, '财务管理部', 22, 0.00, 0, 0.00, 3.00, 176.00, 0.00, '2025-01-31', 104, '10004', '赵磊', 1),
('AR202501014', '2025-01', 14, '009', '苏文博', 1, '技术研发部', 22, 0.00, 0, 0.00, 8.00, 176.00, 0.00, '2025-01-31', 101, '10001', '张伟', 1),
('AR202501015', '2025-01', 15, '010', '林雨萱', 2, '市场销售部', 21, 0.00, 1, 0.00, 3.00, 168.00, 50.00, '2025-01-31', 102, '10002', '李明', 1);

-- 绩效评分数据 (2025年1月)
INSERT INTO `t_performance` (`year_month`, `emp_id`, `emp_no`, `emp_name`, `dept_id`, `work_attitude`, `business_skill`, `work_performance`, `bonus_deduct`, `score`, `grade`, `perf_bonus_ratio`, `manager_id`, `manager_name`, `status`) VALUES 
('2025-01', 1, '10001', '张伟', 1, 95.00, 92.00, 90.00, 5.00, 94.00, 'A', 1.1000, 101, '张伟', 2),
('2025-01', 2, '10002', '李明', 2, 88.00, 86.00, 90.00, 0.00, 88.00, 'B', 1.0000, 102, '李明', 2),
('2025-01', 3, '10003', '王强', 3, 92.00, 90.00, 89.00, 0.00, 90.33, 'A', 1.1000, 103, '王强', 2),
('2025-01', 4, '10004', '赵磊', 4, 85.00, 84.00, 86.00, 0.00, 85.00, 'B', 1.0000, 104, '赵磊', 2),
('2025-01', 5, '10005', '刘洋', 1, 96.00, 94.00, 95.00, 5.00, 96.67, 'S', 1.2000, 101, '张伟', 2),
('2025-01', 6, '001', '王建国', 1, 82.00, 80.00, 84.00, 0.00, 82.00, 'B', 1.0000, 101, '张伟', 2),
('2025-01', 7, '002', '林晓燕', 1, 92.00, 90.00, 92.00, 0.00, 91.33, 'A', 1.1000, 101, '张伟', 2),
('2025-01', 8, '003', '陈志远', 2, 87.00, 85.00, 89.00, 0.00, 87.00, 'B', 1.0000, 102, '李明', 2),
('2025-01', 9, '004', '李雪梅', 2, 84.00, 82.00, 87.00, 0.00, 84.33, 'B', 1.0000, 102, '李明', 2),
('2025-01', 10, '005', '孙浩然', 3, 89.00, 88.00, 90.00, 0.00, 89.00, 'B', 1.0000, 103, '王强', 2),
('2025-01', 11, '006', '周婷婷', 3, 93.00, 92.00, 94.00, 0.00, 93.00, 'A', 1.1000, 103, '王强', 2),
('2025-01', 12, '007', '吴俊杰', 4, 86.00, 85.00, 88.00, 0.00, 86.33, 'B', 1.0000, 104, '赵磊', 2),
('2025-01', 13, '008', '郑美玲', 4, 94.00, 93.00, 95.00, 0.00, 94.00, 'A', 1.1000, 104, '赵磊', 2),
('2025-01', 14, '009', '苏文博', 1, 88.00, 86.00, 91.00, 0.00, 88.33, 'B', 1.0000, 101, '张伟', 2),
('2025-01', 15, '010', '林雨萱', 2, 91.00, 89.00, 93.00, 0.00, 91.00, 'A', 1.1000, 102, '李明', 2);

-- 薪资记录数据 (2025年1月)
INSERT INTO `t_salary_record` (`year_month`, `emp_id`, `emp_no`, `emp_name`, `dept_id`, `dept_name`, `bank_account`, `base_salary`, `overtime_pay`, `perf_bonus`, `allowance`, `gross_salary`, `social_security_emp`, `attend_deduct`, `income_tax`, `total_deduct`, `net_salary`, `calc_status`, `record_date`) VALUES 
('2025-01', 1, '10001', '张伟', 1, '技术研发部', '6222000000000010001', 15000.00, 800.00, 3000.00, 1600.00, 20400.00, 1800.00, 0.00, 1200.00, 3000.00, 17400.00, 2, '2025-01-31'),
('2025-01', 2, '10002', '李明', 2, '市场销售部', '6222000000000010002', 12000.00, 400.00, 2400.00, 1600.00, 16400.00, 1440.00, 50.00, 800.00, 2290.00, 14110.00, 2, '2025-01-31'),
('2025-01', 3, '10003', '王强', 3, '人力资源部', '6222000000000010003', 11000.00, 200.00, 2200.00, 1600.00, 15000.00, 1320.00, 0.00, 700.00, 2020.00, 12980.00, 2, '2025-01-31'),
('2025-01', 4, '10004', '赵磊', 4, '财务管理部', '6222000000000010004', 13000.00, 0.00, 2600.00, 1600.00, 17200.00, 1560.00, 200.00, 900.00, 2660.00, 14540.00, 2, '2025-01-31'),
('2025-01', 5, '10005', '刘洋', 1, '技术研发部', '6222000000000010005', 14000.00, 1200.00, 3360.00, 1600.00, 20160.00, 1680.00, 0.00, 1100.00, 2780.00, 17380.00, 2, '2025-01-31'),
('2025-01', 6, '001', '王建国', 1, '技术研发部', '6222000000000000001', 10000.00, 600.00, 2000.00, 1600.00, 14200.00, 1200.00, 50.00, 500.00, 1750.00, 12450.00, 2, '2025-01-31'),
('2025-01', 7, '002', '林晓燕', 1, '技术研发部', '6222000000000000002', 8500.00, 400.00, 1870.00, 1600.00, 12370.00, 1020.00, 0.00, 350.00, 1370.00, 11000.00, 2, '2025-01-31'),
('2025-01', 8, '003', '陈志远', 2, '市场销售部', '6222000000000000003', 6000.00, 200.00, 1200.00, 1600.00, 9000.00, 720.00, 0.00, 150.00, 870.00, 8130.00, 2, '2025-01-31'),
('2025-01', 9, '004', '李雪梅', 2, '市场销售部', '6222000000000000004', 5500.00, 0.00, 1100.00, 1600.00, 8200.00, 660.00, 200.00, 100.00, 960.00, 7240.00, 2, '2025-01-31'),
('2025-01', 10, '005', '孙浩然', 3, '人力资源部', '6222000000000000005', 7000.00, 200.00, 1400.00, 1600.00, 10200.00, 840.00, 0.00, 200.00, 1040.00, 9160.00, 2, '2025-01-31'),
('2025-01', 11, '006', '周婷婷', 3, '人力资源部', '6222000000000000006', 7500.00, 400.00, 1650.00, 1600.00, 11150.00, 900.00, 0.00, 250.00, 1150.00, 10000.00, 2, '2025-01-31'),
('2025-01', 12, '007', '吴俊杰', 4, '财务管理部', '6222000000000000007', 8000.00, 200.00, 1600.00, 1600.00, 11400.00, 960.00, 50.00, 300.00, 1310.00, 10090.00, 2, '2025-01-31'),
('2025-01', 13, '008', '郑美玲', 4, '财务管理部', '6222000000000000008', 8200.00, 300.00, 1804.00, 1600.00, 11904.00, 984.00, 0.00, 320.00, 1304.00, 10600.00, 2, '2025-01-31'),
('2025-01', 14, '009', '苏文博', 1, '技术研发部', '6222000000000000009', 9000.00, 800.00, 1800.00, 1600.00, 13200.00, 1080.00, 0.00, 400.00, 1480.00, 11720.00, 2, '2025-01-31'),
('2025-01', 15, '010', '林雨萱', 2, '市场销售部', '6222000000000000010', 6500.00, 300.00, 1430.00, 1600.00, 9830.00, 780.00, 50.00, 180.00, 1010.00, 8820.00, 2, '2025-01-31');

-- 公告数据
INSERT INTO `t_announcement` (`id`, `title`, `content`, `pub_user_id`, `pub_user_name`, `target_role`, `is_top`, `status`, `pub_time`) VALUES 
(1, '关于2025年春节放假安排的通知', '各位同事：\n\n根据国家法定节假日安排，现将2025年春节放假安排通知如下：\n\n1. 放假时间：2025年1月28日至2月4日，共8天。\n2. 1月26日（周日）、2月8日（周六）正常上班。\n3. 请各部门提前做好工作安排，确保节前各项工作顺利完成。\n\n祝大家新春快乐！', 1, '系统管理员', 0, 1, 1, '2025-01-15 10:00:00'),
(2, '2024年度优秀员工表彰', '经公司研究决定，对2024年度表现优秀的员工进行表彰：\n\n技术研发部：刘经理\n市场销售部：陈员工\n人力资源部：周员工\n财务管理部：郑员工\n\n希望全体员工向他们学习，在新的一年里再创佳绩！', 1, '系统管理员', 0, 0, 1, '2025-01-10 14:00:00'),
(3, '薪资系统升级公告', '各位同事：\n\n公司薪资管理系统已完成升级，新增以下功能：\n1. 在线查看工资条\n2. 绩效评分查询\n3. 考勤记录查看\n4. 薪资异议反馈\n\n如有问题请联系人力资源部。', 1, '系统管理员', 0, 0, 1, '2025-01-05 09:00:00'),
(4, '关于调整社保缴纳比例的通知', '根据最新政策，自2025年1月起，公司社保缴纳比例调整如下：\n\n养老保险：单位16%，个人8%\n医疗保险：单位8%，个人2%\n失业保险：单位0.5%，个人0.5%\n工伤保险：单位0.5%，个人不缴\n住房公积金：单位5%-12%，个人5%-12%\n\n请知悉。', 1, '系统管理员', 0, 0, 1, '2025-01-03 11:00:00'),
(5, '新员工入职培训通知', '各位新入职同事：\n\n公司将于2025年1月20日举办新员工入职培训，请以下员工准时参加：\n\n时间：2025年1月20日 9:00-17:00\n地点：公司三楼会议室\n\n请携带身份证原件及复印件。', 1, '系统管理员', 0, 0, 1, '2025-01-18 16:00:00');

-- 考勤申请数据
INSERT INTO `t_attendance_apply` (`attendance_id`, `emp_id`, `emp_no`, `emp_name`, `dept_id`, `apply_type`, `apply_date`, `reason`, `status`, `review_user_id`, `review_user_name`, `review_comment`, `review_time`) VALUES 
(NULL, 6, '001', '王建国', 1, 1, '2025-01-15', '因家中急事忘记打卡，申请补签', 1, 101, '张伟', '情况属实，同意补签', '2025-01-16 09:30:00'),
(NULL, 9, '004', '李雪梅', 2, 3, '2025-01-20', '周末加班处理客户订单，申请加班', 1, 102, '李明', '同意加班申请', '2025-01-21 10:00:00'),
(NULL, 8, '003', '陈志远', 2, 2, '2025-01-25', '因身体不适请假一天，申请销假', 2, 102, '李明', '请假天数已核实', '2025-01-26 14:00:00'),
(NULL, 12, '007', '吴俊杰', 4, 4, '2025-01-28', '考勤记录显示迟到，但实际已按时到岗，申请异议', 0, NULL, NULL, NULL, NULL),
(NULL, 14, '009', '苏文博', 1, 1, '2025-01-30', '系统故障导致打卡失败，申请补签', 0, NULL, NULL, NULL, NULL);

-- 异常上报数据
INSERT INTO `t_anomaly_report` (`report_type`, `reporter_id`, `reporter_name`, `emp_id`, `emp_no`, `emp_name`, `year_month`, `title`, `description`, `status`, `process_result`, `processor_id`, `processor_name`, `process_time`) VALUES 
(1, 101, '张伟', 6, '001', '王建国', '2025-01', '考勤数据异常', '员工王建国1月15日考勤记录缺失，实际已到岗工作', 2, '已核实并补录考勤记录', 1, '系统管理员', '2025-01-17 11:00:00'),
(2, 102, '李明', 8, '003', '陈志远', '2025-01', '薪资计算异常', '员工陈志远1月份绩效奖金计算有误，应发1200元，实发1000元', 2, '已重新核算并补发差额200元', 1, '系统管理员', '2025-01-20 15:30:00'),
(3, 103, '王强', NULL, NULL, NULL, '2025-01', '系统数据异常', '人力资源部员工档案数据与考勤系统数据不一致', 1, '正在核查数据差异', 1, '系统管理员', '2025-01-22 09:00:00'),
(1, 104, '赵磊', 12, '007', '吴俊杰', '2025-01', '加班时长统计异常', '员工吴俊杰1月份加班时长统计错误，少计2小时', 0, NULL, NULL, NULL, NULL),
(2, 101, '张伟', 14, '009', '苏文博', '2025-01', '社保扣款异常', '员工苏文博1月份社保扣款金额与实际缴纳金额不符', 0, NULL, NULL, NULL, NULL);

-- 薪资反馈数据
INSERT INTO `t_salary_feedback` (`feedback_type`, `emp_id`, `emp_no`, `emp_name`, `salary_record_id`, `year_month`, `title`, `content`, `status`, `reply_content`, `reply_user_id`, `reply_user_name`, `reply_time`) VALUES 
(1, 6, '001', '王建国', 6, '2025-01', '绩效奖金计算疑问', '您好，我1月份的绩效评分为82分，绩效奖金为2000元，请问绩效奖金是如何计算的？', 2, '绩效奖金=基本工资×绩效系数×绩效奖金比例。您的绩效系数为1.0，绩效奖金比例为20%，即10000×1.0×0.2=2000元。', 1, '系统管理员', '2025-02-05 10:00:00'),
(2, 8, '003', '陈志远', 8, '2025-01', '个税扣除疑问', '我1月份工资6000元，扣除个税150元，请问是如何计算的？', 2, '个税采用累计预扣法计算。应纳税所得额=工资-社保-起征点-专项扣除。具体情况已通过邮件详细说明。', 1, '系统管理员', '2025-02-05 11:30:00'),
(1, 12, '007', '吴俊杰', 12, '2025-01', '考勤扣款异议', '我1月份只有1次迟到，扣款50元，但实际那天是因为电梯故障导致迟到，请核实。', 1, '正在核实情况，请提供相关证明材料。', 4, '赵磊', '2025-02-06 09:00:00'),
(3, 9, '004', '李雪梅', 9, '2025-01', '系统操作反馈', '建议在工资条中增加各项扣款的详细说明，方便员工理解。', 3, '感谢您的建议，我们会在后续版本中优化工资条展示功能。', 1, '系统管理员', '2025-02-06 14:00:00'),
(1, 14, '009', '苏文博', 14, '2025-01', '加班费计算疑问', '我1月份加班8小时，加班费800元，请问加班费是如何计算的？', 0, NULL, NULL, NULL, NULL);

-- 薪资发放数据
INSERT INTO `t_salary_payment` (`salary_record_id`, `emp_id`, `emp_no`, `emp_name`, `year_month`, `net_salary`, `bank_account`, `bank_name`, `pay_date`, `pay_method`, `pay_status`, `operator_id`, `operator_name`) VALUES 
(1, 1, '10001', '张伟', '2025-01', 17400.00, '6222000000000010001', '中国工商银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(2, 2, '10002', '李明', '2025-01', 14110.00, '6222000000000010002', '中国建设银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(3, 3, '10003', '王强', '2025-01', 12980.00, '6222000000000010003', '中国农业银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(4, 4, '10004', '赵磊', '2025-01', 14540.00, '6222000000000010004', '中国银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(5, 5, '10005', '刘洋', '2025-01', 17380.00, '6222000000000010005', '中国工商银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(6, 6, '001', '王建国', '2025-01', 12450.00, '6222000000000000001', '中国工商银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(7, 7, '002', '林晓燕', '2025-01', 11000.00, '6222000000000000002', '中国建设银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(8, 8, '003', '陈志远', '2025-01', 8130.00, '6222000000000000003', '中国农业银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(9, 9, '004', '李雪梅', '2025-01', 7240.00, '6222000000000000004', '中国银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(10, 10, '005', '孙浩然', '2025-01', 9160.00, '6222000000000000005', '中国工商银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(11, 11, '006', '周婷婷', '2025-01', 10000.00, '6222000000000000006', '中国建设银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(12, 12, '007', '吴俊杰', '2025-01', 10090.00, '6222000000000000007', '中国农业银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(13, 13, '008', '郑美玲', '2025-01', 10600.00, '6222000000000000008', '中国银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(14, 14, '009', '苏文博', '2025-01', 11720.00, '6222000000000000009', '中国工商银行', '2025-02-10', 1, 2, 1, '系统管理员'),
(15, 15, '010', '林雨萱', '2025-01', 8820.00, '6222000000000000010', '中国建设银行', '2025-02-10', 1, 2, 1, '系统管理员');

-- 个税累计数据
INSERT INTO `t_tax_accumulate` (`emp_id`, `tax_year`, `year_month`, `month_taxable_income`, `month_tax`, `month_social_security`, `month_fund`, `month_special_deduct`, `accum_taxable_income`, `accum_tax`, `accum_gross`, `accum_social_security`, `accum_special_deduct`, `salary_record_id`) VALUES 
(1, '2025', '2025-01', 8200.00, 1200.00, 1800.00, 1200.00, 1000.00, 8200.00, 1200.00, 20400.00, 1800.00, 1000.00, 1),
(2, '2025', '2025-01', 6100.00, 800.00, 1440.00, 960.00, 1000.00, 6100.00, 800.00, 16400.00, 1440.00, 1000.00, 2),
(3, '2025', '2025-01', 5200.00, 700.00, 1320.00, 880.00, 1000.00, 5200.00, 700.00, 15000.00, 1320.00, 1000.00, 3),
(4, '2025', '2025-01', 6400.00, 900.00, 1560.00, 1040.00, 1000.00, 6400.00, 900.00, 17200.00, 1560.00, 1000.00, 4),
(5, '2025', '2025-01', 7300.00, 1100.00, 1680.00, 1120.00, 1000.00, 7300.00, 1100.00, 20160.00, 1680.00, 1000.00, 5),
(6, '2025', '2025-01', 3800.00, 500.00, 1200.00, 800.00, 1000.00, 3800.00, 500.00, 14200.00, 1200.00, 1000.00, 6),
(7, '2025', '2025-01', 2750.00, 350.00, 1020.00, 680.00, 1000.00, 2750.00, 350.00, 12370.00, 1020.00, 1000.00, 7),
(8, '2025', '2025-01', 1300.00, 150.00, 720.00, 480.00, 1000.00, 1300.00, 150.00, 9000.00, 720.00, 1000.00, 8),
(9, '2025', '2025-01', 600.00, 100.00, 660.00, 440.00, 1000.00, 600.00, 100.00, 8200.00, 660.00, 1000.00, 9),
(10, '2025', '2025-01', 1800.00, 200.00, 840.00, 560.00, 1000.00, 1800.00, 200.00, 10200.00, 840.00, 1000.00, 10);

-- 社保配置数据
INSERT INTO `t_social_security_config` (`config_name`, `pension_rate`, `medical_rate`, `unemployment_rate`, `injury_rate`, `maternity_rate`, `fund_rate`, `base_min`, `base_max`, `effective_date`, `is_active`) VALUES 
('2025年社保配置', 0.1600, 0.0800, 0.0050, 0.0050, 0.0000, 0.1200, 3000.00, 30000.00, '2025-01-01', 1),
('2024年社保配置', 0.1600, 0.0800, 0.0050, 0.0050, 0.0000, 0.1200, 2800.00, 28000.00, '2024-01-01', 0);

SET FOREIGN_KEY_CHECKS = 1;
