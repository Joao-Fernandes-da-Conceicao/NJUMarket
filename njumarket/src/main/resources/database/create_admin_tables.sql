-- 管理员表创建脚本
-- 用于存储内部管理员账号信息

-- 创建管理员表
CREATE TABLE IF NOT EXISTS `admins` (
    `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `department` VARCHAR(50) DEFAULT NULL COMMENT '部门',
    `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
    `admin_level` VARCHAR(20) NOT NULL DEFAULT 'administrator' COMMENT '管理员级别：system-系统管理员，administrator-普通管理员',
    `permissions` TEXT DEFAULT NULL COMMENT '权限列表（JSON格式）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `account_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态：ACTIVE-活跃，SUSPENDED-暂停，BANNED-禁用',
    `login_count` INT NOT NULL DEFAULT 0 COMMENT '登录次数',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`admin_id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_admin_level` (`admin_level`),
    INDEX `idx_account_status` (`account_status`),
    INDEX `idx_department` (`department`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 插入默认系统管理员账号
-- 用户名: system, 密码: system123 (需要在实际部署时修改)
INSERT INTO `admins` (
    `admin_id`, 
    `username`, 
    `password`, 
    `real_name`, 
    `email`, 
    `department`, 
    `position`, 
    `admin_level`, 
    `permissions`, 
    `account_status`, 
    `remark`
) VALUES (
    'ADMIN_SYSTEM_001',
    'system',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', -- system123的BCrypt加密
    '系统管理员',
    'system@njumarket.com',
    '技术部',
    '系统管理员',
    'system',
    'user:view,user:edit,user:delete,commodity:view,commodity:edit,commodity:delete,order:view,order:edit,complaint:view,complaint:edit,admin:view,admin:edit,admin:delete,system:config',
    'ACTIVE',
    '系统默认管理员账号，拥有所有权限，请及时修改密码'
);

-- 插入示例普通管理员账号
-- 用户名: manager, 密码: manager123
INSERT INTO `admins` (
    `admin_id`, 
    `username`, 
    `password`, 
    `real_name`, 
    `email`, 
    `department`, 
    `position`, 
    `admin_level`, 
    `permissions`, 
    `account_status`, 
    `remark`
) VALUES (
    'ADMIN_ADMINISTRATOR_001',
    'manager',
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', -- manager123的BCrypt加密
    '运营经理',
    'manager@njumarket.com',
    '运营部',
    '运营经理',
    'administrator',
    'user:view,commodity:view,commodity:edit,order:view,complaint:view',
    'ACTIVE',
    '运营部门管理员账号'
);

-- 创建管理员操作日志表（可选，用于记录管理员操作）
CREATE TABLE IF NOT EXISTS `admin_operation_logs` (
    `log_id` VARCHAR(50) NOT NULL COMMENT '日志ID',
    `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `operation_desc` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `target_id` VARCHAR(50) DEFAULT NULL COMMENT '目标对象ID',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标对象类型',
    `operation_data` TEXT DEFAULT NULL COMMENT '操作数据（JSON格式）',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`log_id`),
    INDEX `idx_admin_id` (`admin_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`admin_id`) REFERENCES `admins`(`admin_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- 创建管理员会话表（可选，用于管理管理员登录会话）
CREATE TABLE IF NOT EXISTS `admin_sessions` (
    `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
    `admin_id` VARCHAR(50) NOT NULL COMMENT '管理员ID',
    `token` VARCHAR(500) NOT NULL COMMENT 'JWT Token',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `last_activity_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活动时间',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否活跃',
    PRIMARY KEY (`session_id`),
    INDEX `idx_admin_id` (`admin_id`),
    INDEX `idx_token` (`token`),
    INDEX `idx_expire_time` (`expire_time`),
    INDEX `idx_is_active` (`is_active`),
    FOREIGN KEY (`admin_id`) REFERENCES `admins`(`admin_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员会话表';

-- 添加表注释
ALTER TABLE `admins` COMMENT = '管理员表 - 存储内部管理员账号信息';
ALTER TABLE `admin_operation_logs` COMMENT = '管理员操作日志表 - 记录管理员的操作行为';
ALTER TABLE `admin_sessions` COMMENT = '管理员会话表 - 管理管理员登录会话';

-- 创建视图：管理员基本信息视图
CREATE OR REPLACE VIEW `v_admin_basic_info` AS
SELECT 
    `admin_id`,
    `username`,
    `real_name`,
    `email`,
    `department`,
    `position`,
    `admin_level`,
    `account_status`,
    `create_time`,
    `last_login_time`,
    `login_count`
FROM `admins`
WHERE `account_status` = 'ACTIVE';

-- 创建视图：管理员统计信息视图
CREATE OR REPLACE VIEW `v_admin_statistics` AS
SELECT 
    COUNT(*) as total_admins,
    SUM(CASE WHEN `account_status` = 'ACTIVE' THEN 1 ELSE 0 END) as active_admins,
    SUM(CASE WHEN `admin_level` = 'system' THEN 1 ELSE 0 END) as system_admins,
    SUM(CASE WHEN `admin_level` = 'administrator' THEN 1 ELSE 0 END) as administrator_count,
    SUM(`login_count`) as total_login_count
FROM `admins`;

-- 插入完成后显示创建结果
SELECT '管理员表创建完成' as message;
SELECT COUNT(*) as admin_count FROM `admins`;
SELECT * FROM `v_admin_statistics`;
