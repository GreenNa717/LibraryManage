-- =============================================
-- Library 图书馆管理系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0
-- 数据库名: librarymanage
-- =============================================

CREATE DATABASE IF NOT EXISTS librarymanage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE librarymanage;

-- ---------------------------------------------
-- 删除表（按依赖顺序）
-- ---------------------------------------------
DROP TABLE IF EXISTS borrow_record;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS book_copy;
DROP TABLE IF EXISTS book_info;
DROP TABLE IF EXISTS isbn_metadata;
DROP TABLE IF EXISTS shelf_location;
DROP TABLE IF EXISTS book_category;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------
-- 1. 用户表（超级管理员 / 协管员 / 普通用户 / 游客）
-- ---------------------------------------------
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    nickname VARCHAR(50) COMMENT '昵称',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar LONGTEXT COMMENT '头像（URL或Base64）',
    role TINYINT NOT NULL DEFAULT 1 COMMENT '0:超级管理员, 1:普通用户, 2:协管员, 3:游客',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:正常, 0:封禁',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------
-- 2. 图书分类表
-- ---------------------------------------------
CREATE TABLE book_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序值（越大越靠前）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (category_name),
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

-- ---------------------------------------------
-- 3. 库位表
-- ---------------------------------------------
CREATE TABLE shelf_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(50) NOT NULL COMMENT '库位名称',
    category_id BIGINT COMMENT '关联分类ID',
    max_capacity INT DEFAULT 100 COMMENT '最大容量（册）',
    current_count INT DEFAULT 0 COMMENT '当前存放数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_location_name (location_name),
    INDEX idx_category (category_id),
    FOREIGN KEY (category_id) REFERENCES book_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位表';

-- ---------------------------------------------
-- 4. 图书信息表
-- ---------------------------------------------
CREATE TABLE book_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    isbn VARCHAR(20) UNIQUE COMMENT 'ISBN编号',
    title VARCHAR(100) NOT NULL COMMENT '书名',
    author VARCHAR(50) COMMENT '作者',
    category_id BIGINT COMMENT '分类ID',
    location_id BIGINT COMMENT '库位ID',
    cover_image VARCHAR(255) COMMENT '封面图片URL',
    description TEXT COMMENT '图书简介',
    total_stock INT DEFAULT 0 COMMENT '总库存',
    current_stock INT DEFAULT 0 COMMENT '当前可借库存',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_category (category_id),
    INDEX idx_isbn (isbn),
    INDEX idx_location (location_id),
    FOREIGN KEY (category_id) REFERENCES book_category(id) ON DELETE SET NULL,
    FOREIGN KEY (location_id) REFERENCES shelf_location(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书信息表';

-- ---------------------------------------------
-- 4.1 ISBN元数据表（本地兜底资料库）
-- ---------------------------------------------
CREATE TABLE isbn_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    isbn VARCHAR(20) NOT NULL COMMENT 'ISBN编号（建议保存为13位数字）',
    title VARCHAR(100) NOT NULL COMMENT '书名',
    author VARCHAR(100) COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    publish_date VARCHAR(30) COMMENT '出版日期',
    cover_image VARCHAR(255) COMMENT '封面图片URL',
    source VARCHAR(50) DEFAULT 'local-metadata' COMMENT '数据来源',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_isbn_metadata_isbn (isbn),
    INDEX idx_isbn_metadata_title (title),
    INDEX idx_isbn_metadata_author (author)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ISBN本地元数据表';

-- ---------------------------------------------
-- 5. 图书副本表（每本实体书一条记录）
-- ---------------------------------------------
CREATE TABLE book_copy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_info_id BIGINT NOT NULL COMMENT '关联图书ID',
    copy_code VARCHAR(50) NOT NULL UNIQUE COMMENT '副本编号，如 978-7-111-54742-5-001',
    status TINYINT DEFAULT 0 COMMENT '0:在馆可借, 1:已借出, 2:丢失',
    location_id BIGINT COMMENT '库位ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_book (book_info_id),
    INDEX idx_code (copy_code),
    INDEX idx_status (status),
    FOREIGN KEY (book_info_id) REFERENCES book_info(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES shelf_location(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书副本表';

-- ---------------------------------------------
-- 6. 借阅记录表
-- ---------------------------------------------
CREATE TABLE borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    copy_id BIGINT COMMENT '副本ID（关联book_copy.id）',
    borrow_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    due_time DATETIME NOT NULL COMMENT '应还时间',
    return_time DATETIME COMMENT '实际归还时间',
    status TINYINT DEFAULT 0 COMMENT '0:借阅中, 1:已归还, 2:已逾期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_book (book_id),
    INDEX idx_copy (copy_id),
    INDEX idx_status (status),
    INDEX idx_borrow_time (borrow_time),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES book_info(id) ON DELETE CASCADE,
    FOREIGN KEY (copy_id) REFERENCES book_copy(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ---------------------------------------------
-- 7. 操作日志表
-- ---------------------------------------------
CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    action_type VARCHAR(30) NOT NULL COMMENT '操作类型：BORROW/RETURN/ADD_STOCK/NEW_BOOK/EDIT_BOOK/DELETE_BOOK/LOST_MARK',
    target_desc VARCHAR(200) COMMENT '操作对象描述',
    detail VARCHAR(500) COMMENT '操作详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator_id),
    INDEX idx_action (action_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 超级管理员账号（密码: admin123）
INSERT INTO sys_user (username, password, real_name, nickname, email, role, status) VALUES
('admin', '$2b$10$vB7WfFDnVJdOiA7IwUIEb.OQNXW0mdTMcamxrrQXZ0EdcOYyIJBgK', '系统管理员', '馆长', 'admin@library.local', 0, 1);

-- 协管员账号（密码: admin123）
INSERT INTO sys_user (username, password, real_name, nickname, phone, email, role, status) VALUES
('assistant001', '$2b$10$vB7WfFDnVJdOiA7IwUIEb.OQNXW0mdTMcamxrrQXZ0EdcOYyIJBgK', '协管员A', '书库助手A', '13800138010', 'assistant001@library.local', 2, 1);

-- 测试普通用户账号（密码: user123）
INSERT INTO sys_user (username, password, real_name, nickname, phone, email, role, status) VALUES
('reader001', '$2b$10$TuiA.VaJ0pe0bduSGJhPu.TOi2mHYB0F9bJZKsn1OI97Z4Llrtghy', '张三', '小张', '13800138001', 'reader001@library.local', 1, 1),
('reader002', '$2b$10$TuiA.VaJ0pe0bduSGJhPu.TOi2mHYB0F9bJZKsn1OI97Z4Llrtghy', '李四', '小李', '13800138002', 'reader002@library.local', 1, 1);

-- 测试游客账号（密码: user123）
INSERT INTO sys_user (username, password, real_name, nickname, phone, email, role, status) VALUES
('guest001', '$2b$10$TuiA.VaJ0pe0bduSGJhPu.TOi2mHYB0F9bJZKsn1OI97Z4Llrtghy', '游客账号', '游客', '13800138003', 'guest001@library.local', 3, 1);

-- 图书分类
INSERT INTO book_category (category_name, sort) VALUES
('计算机', 10), ('文学', 9), ('历史', 8), ('经济', 7), ('哲学', 6), ('艺术', 5);

-- 库位
INSERT INTO shelf_location (location_name, category_id, max_capacity, current_count) VALUES
('A区计算机架', 1, 100, 9), ('A区文学架', 2, 100, 5), ('B区历史架', 3, 100, 4),
('B区经济架', 4, 100, 3), ('C区哲学架', 5, 100, 0), ('C区艺术架', 6, 100, 0);

-- 图书（stock 与 borrow_record 保持一致）
INSERT INTO book_info (isbn, title, author, category_id, location_id, description, total_stock, current_stock) VALUES
('978-7-111-54742-5', '深入理解计算机系统', 'Randal E. Bryant', 1, 1, '计算机系统经典教材。', 5, 5),
('978-7-115-44855-3', '算法导论（第4版）', 'Thomas H. Cormen', 1, 1, '算法领域权威教材。', 4, 3),
('978-7-5322-4650-1', '活着', '余华', 2, 2, '描写一个人在历史变迁中的命运。', 3, 3),
('978-7-5399-58453-7', '百年孤独', '加西亚·马尔克斯', 2, 2, '魔幻现实主义文学代表作。', 2, 1),
('978-7-5322-2650-8', '明朝那些事儿（壹）', '当年明月', 3, 3, '幽默讲述明朝历史。', 4, 3),
('978-7-111-48911-6', '富爸爸穷爸爸', '罗伯特·清崎', 4, 4, '财商教育经典。', 3, 2);

-- 本地ISBN资料库（外部源不可用或中文书未覆盖时兜底）
INSERT INTO isbn_metadata (isbn, title, author, publisher, publish_date, source) VALUES
('9787111547425', '深入理解计算机系统', 'Randal E. Bryant', '机械工业出版社', NULL, 'local-metadata'),
('9787115448553', '算法导论（第4版）', 'Thomas H. Cormen', '人民邮电出版社', NULL, 'local-metadata'),
('9787532246501', '活着', '余华', '上海文艺出版社', NULL, 'local-metadata'),
('9787539958457', '百年孤独', '加西亚·马尔克斯', '南海出版公司', NULL, 'local-metadata'),
('9787532226508', '明朝那些事儿（壹）', '当年明月', '上海锦绣文章出版社', NULL, 'local-metadata'),
('9787111489116', '富爸爸穷爸爸', '罗伯特·清崎', '四川人民出版社', NULL, 'local-metadata');

-- 图书副本（每本实体书一条记录）
-- Book 1: 深入理解计算机系统，5本，全部在馆
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(1, '978-7-111-54742-5-001', 0, 1),
(1, '978-7-111-54742-5-002', 0, 1),
(1, '978-7-111-54742-5-003', 0, 1),
(1, '978-7-111-54742-5-004', 0, 1),
(1, '978-7-111-54742-5-005', 0, 1);

-- Book 2: 算法导论，4本，1本借出
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(2, '978-7-115-44855-3-001', 1, 1),
(2, '978-7-115-44855-3-002', 0, 1),
(2, '978-7-115-44855-3-003', 0, 1),
(2, '978-7-115-44855-3-004', 0, 1);

-- Book 3: 活着，3本，全部在馆
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(3, '978-7-5322-4650-1-001', 0, 2),
(3, '978-7-5322-4650-1-002', 0, 2),
(3, '978-7-5322-4650-1-003', 0, 2);

-- Book 4: 百年孤独，2本，1本逾期
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(4, '978-7-5399-58453-7-001', 1, 2),
(4, '978-7-5399-58453-7-002', 0, 2);

-- Book 5: 明朝那些事儿，4本，1本借出
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(5, '978-7-5322-2650-8-001', 1, 3),
(5, '978-7-5322-2650-8-002', 0, 3),
(5, '978-7-5322-2650-8-003', 0, 3),
(5, '978-7-5322-2650-8-004', 0, 3);

-- Book 6: 富爸爸穷爸爸，3本，1本逾期
INSERT INTO book_copy (book_info_id, copy_code, status, location_id) VALUES
(6, '978-7-111-48911-6-001', 1, 4),
(6, '978-7-111-48911-6-002', 0, 4),
(6, '978-7-111-48911-6-003', 0, 4);

-- 借阅记录（关联具体副本）
-- 已归还
INSERT INTO borrow_record (user_id, book_id, copy_id, borrow_time, due_time, return_time, status) VALUES
(3, 1, 1, '2026-04-01 10:00:00', '2026-04-15 23:59:59', '2026-04-12 16:30:00', 1),
(4, 3, 10, '2026-04-05 14:20:00', '2026-04-19 23:59:59', '2026-04-18 11:00:00', 1),
-- 借阅中（未超期）
(3, 2, 6, '2026-05-01 09:00:00', '2026-05-15 23:59:59', NULL, 0),
(4, 5, 15, '2026-05-03 15:00:00', '2026-05-17 23:59:59', NULL, 0),
-- 逾期
(3, 4, 13, '2026-04-10 08:30:00', '2026-04-24 23:59:59', NULL, 2),
(4, 6, 19, '2026-04-15 10:00:00', '2026-04-29 23:59:59', NULL, 2);

-- 操作日志
INSERT INTO operation_log (operator_id, operator_name, action_type, target_desc, detail, create_time) VALUES
(1, '系统管理员', 'NEW_BOOK', '《深入理解计算机系统》', '新增入库 5 册', '2026-04-01 09:00:00'),
(1, '系统管理员', 'NEW_BOOK', '《算法导论（第4版）》', '新增入库 4 册', '2026-04-01 09:10:00'),
(1, '系统管理员', 'BORROW', '《深入理解计算机系统》 #001', '借出给 张三', '2026-04-01 10:00:00'),
(1, '系统管理员', 'BORROW', '《活着》 #001', '借出给 李四', '2026-04-05 14:20:00'),
(1, '系统管理员', 'RETURN', '《深入理解计算机系统》 #001', '张三 归还', '2026-04-12 16:30:00'),
(1, '系统管理员', 'RETURN', '《活着》 #001', '李四 归还', '2026-04-18 11:00:00'),
(1, '系统管理员', 'BORROW', '《算法导论》 #001', '借出给 张三', '2026-05-01 09:00:00'),
(1, '系统管理员', 'BORROW', '《明朝那些事儿》 #001', '借出给 李四', '2026-05-03 15:00:00');

-- ---------------------------------------------
-- 数据一致性回算（与当前后端逻辑对齐）
-- ---------------------------------------------
-- 1) 根据副本状态回算 book_info 总库存/可借库存
UPDATE book_info b
LEFT JOIN (
    SELECT
        book_info_id,
        COUNT(*) AS total_cnt,
        SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS available_cnt
    FROM book_copy
    GROUP BY book_info_id
) c ON b.id = c.book_info_id
SET
    b.total_stock = COALESCE(c.total_cnt, 0),
    b.current_stock = COALESCE(c.available_cnt, 0);

-- 2) 根据副本数量回算库位 current_count
UPDATE shelf_location s
LEFT JOIN (
    SELECT location_id, COUNT(*) AS cnt
    FROM book_copy
    WHERE location_id IS NOT NULL
    GROUP BY location_id
) x ON s.id = x.location_id
SET s.current_count = COALESCE(x.cnt, 0);
