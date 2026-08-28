-- Spring project database initialization script.
-- Keep this file idempotent so it can be executed repeatedly during development.

CREATE DATABASE IF NOT EXISTS `spring`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `spring`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建user表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(32) NOT NULL COMMENT '登录用户名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码密文',
    `nickname` VARCHAR(32) NOT NULL COMMENT '用户昵称',
    `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色：USER、ADMIN',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED、DISABLED',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    CONSTRAINT `chk_user_role` CHECK (`role` IN ('USER', 'ADMIN')),
    CONSTRAINT `chk_user_status` CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户表';

-- 创建category表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
    `sort` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED、DISABLED',
    `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `active_name` VARCHAR(64) GENERATED ALWAYS AS (
        CASE WHEN `deleted` = 0 THEN `name` ELSE NULL END
    ) VIRTUAL COMMENT '未删除分类名称，用于唯一约束',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_active_name` (`active_name`),
    KEY `idx_category_user_list` (`deleted`, `status`, `sort`, `id`),
    CONSTRAINT `chk_category_status` CHECK (`status` IN ('ENABLED', 'DISABLED')),
    CONSTRAINT `chk_category_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品分类表';

-- 创建product表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品 ID',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '所属分类 ID',
    `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
    `image_url` VARCHAR(512) NULL COMMENT '商品主图 URL',
    `description` TEXT NULL COMMENT '商品详情描述',
    `price` DECIMAL(12, 2) NOT NULL COMMENT '商品价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '当前可售库存',
    `sales` INT NOT NULL DEFAULT 0 COMMENT '累计成功支付销量',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        COMMENT '状态：DRAFT、ON_SALE、OFF_SALE',
    `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_category_deleted` (`category_id`, `deleted`, `id`),
    KEY `idx_product_public_default` (`deleted`, `status`, `id`),
    KEY `idx_product_public_price` (`deleted`, `status`, `price`, `id`),
    KEY `idx_product_category_price`
        (`deleted`, `status`, `category_id`, `price`, `id`),
    CONSTRAINT `fk_product_category`
        FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_product_price` CHECK (`price` >= 0),
    CONSTRAINT `chk_product_stock` CHECK (`stock` >= 0),
    CONSTRAINT `chk_product_sales` CHECK (`sales` >= 0),
    CONSTRAINT `chk_product_status`
        CHECK (`status` IN ('DRAFT', 'ON_SALE', 'OFF_SALE')),
    CONSTRAINT `chk_product_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品表';

-- 创建stock_log表
CREATE TABLE IF NOT EXISTS `stock_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存日志 ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品 ID',
    `change_quantity` INT NOT NULL COMMENT '库存变动量，正数增加、负数减少',
    `before_stock` INT NOT NULL COMMENT '变动前库存',
    `after_stock` INT NOT NULL COMMENT '变动后库存',
    `reason` VARCHAR(255) NOT NULL COMMENT '库存变动原因',
    `business_no` VARCHAR(64) NULL COMMENT '关联业务单号，如订单号；无外部业务时可为空',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_stock_log_product_created` (`product_id`, `created_at`, `id`),
    KEY `idx_stock_log_business_no` (`business_no`),
    CONSTRAINT `fk_stock_log_product`
        FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_stock_log_change_quantity` CHECK (`change_quantity` <> 0),
    CONSTRAINT `chk_stock_log_before_stock` CHECK (`before_stock` >= 0),
    CONSTRAINT `chk_stock_log_after_stock` CHECK (`after_stock` >= 0),
    CONSTRAINT `chk_stock_log_stock_change`
        CHECK (`after_stock` = `before_stock` + `change_quantity`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '库存变动日志表';

-- 创建stock_operation_log表（库存操作幂等流水表）
CREATE TABLE IF NOT EXISTS `stock_operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
    `operation_type` VARCHAR(32) NOT NULL COMMENT '操作类型：DEDUCT-扣减，RESTORE-恢复',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING-处理中，SUCCESS-成功，FAILED-失败',
    `owner_token` VARCHAR(64) NULL COMMENT '当前处理者的 fencing token',
    `detail` TEXT NULL COMMENT '操作详情快照（JSON 格式）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no_operation` (`order_no`, `operation_type`),
    KEY `idx_stock_op_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '库存操作幂等流水表';

-- 创建user_address表
CREATE TABLE IF NOT EXISTS `user_address` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址 ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户 ID',
    `receiver_name` VARCHAR(32) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    `province` VARCHAR(64) NOT NULL COMMENT '省/直辖市',
    `city` VARCHAR(64) NOT NULL COMMENT '市',
    `district` VARCHAR(64) NOT NULL COMMENT '区/县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `is_default_unique` TINYINT GENERATED ALWAYS AS (
        CASE WHEN `deleted` = 0 AND `is_default` = 1 THEN 1 ELSE NULL END
    ) VIRTUAL COMMENT '用于未删除默认地址唯一约束',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_default_address` (`user_id`, `is_default_unique`),
    KEY `idx_user_address_list` (`user_id`, `deleted`, `id`),
    CONSTRAINT `fk_user_address_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_user_address_is_default` CHECK (`is_default` IN (0, 1)),
    CONSTRAINT `chk_user_address_deleted` CHECK (`deleted` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户收货地址表';

-- 创建orders订单主表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单 ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '对外订单号',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '下单用户 ID',
    `idempotency_token` VARCHAR(64) NULL COMMENT '用户维度的下单幂等 token',
    `total_amount` DECIMAL(12, 2) NOT NULL COMMENT '订单总金额',
    `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',
    `expire_at` DATETIME(3) NOT NULL COMMENT '待付款截止时间',
    `paid_at` DATETIME(3) NULL COMMENT '支付成功时间',
    `shipping_company` VARCHAR(64) NULL COMMENT '物流公司',
    `tracking_number` VARCHAR(64) NULL COMMENT '物流单号',
    `shipped_at` DATETIME(3) NULL COMMENT '发货时间',
    `completed_at` DATETIME(3) NULL COMMENT '确认收货时间',
    `cancelled_at` DATETIME(3) NULL COMMENT '主动取消或超时取消时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    UNIQUE KEY `uk_orders_user_idempotency` (`user_id`, `idempotency_token`),
    KEY `idx_orders_user_created` (`user_id`, `created_at`, `id`),
    KEY `idx_orders_user_status_created`
        (`user_id`, `status`, `created_at`, `id`),
    KEY `idx_orders_status_expire` (`status`, `expire_at`, `id`),
    CONSTRAINT `fk_orders_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_orders_total_amount` CHECK (`total_amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单主表';

-- 创建order_item商品快照表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单商品快照 ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `product_id` BIGINT UNSIGNED NOT NULL COMMENT '下单时的商品 ID',
    `product_name` VARCHAR(128) NOT NULL COMMENT '下单时的商品名称',
    `product_image_url` VARCHAR(512) NULL COMMENT '下单时的商品主图 URL',
    `unit_price` DECIMAL(12, 2) NOT NULL COMMENT '下单时的商品单价',
    `quantity` INT UNSIGNED NOT NULL COMMENT '购买数量',
    `subtotal_amount` DECIMAL(12, 2) NOT NULL COMMENT '商品小计金额',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_item_order_product` (`order_id`, `product_id`),
    KEY `idx_order_item_order` (`order_id`, `id`),
    CONSTRAINT `fk_order_item_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_order_item_unit_price` CHECK (`unit_price` >= 0),
    CONSTRAINT `chk_order_item_quantity` CHECK (`quantity` BETWEEN 1 AND 99),
    CONSTRAINT `chk_order_item_subtotal`
        CHECK (`subtotal_amount` = `unit_price` * `quantity`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单商品快照表';

-- 创建order_address地址快照表
CREATE TABLE IF NOT EXISTS `order_address` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单地址快照 ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `source_address_id` BIGINT UNSIGNED NOT NULL COMMENT '下单时选择的用户地址 ID',
    `receiver_name` VARCHAR(32) NOT NULL COMMENT '收货人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    `province` VARCHAR(64) NOT NULL COMMENT '省或直辖市',
    `city` VARCHAR(64) NOT NULL COMMENT '市',
    `district` VARCHAR(64) NOT NULL COMMENT '区或县',
    `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_address_order` (`order_id`),
    CONSTRAINT `fk_order_address_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单地址快照表';

-- 创建order_operate_log订单操作日志表
CREATE TABLE IF NOT EXISTS `order_operate_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单操作日志 ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单 ID',
    `operator_type` VARCHAR(16) NOT NULL COMMENT '操作者类型：USER、ADMIN、SYSTEM',
    `operator_id` BIGINT UNSIGNED NULL COMMENT '用户或管理员 ID，系统操作时为空',
    `operator_name` VARCHAR(64) NOT NULL COMMENT '操作者名称快照',
    `operation` VARCHAR(32) NOT NULL COMMENT '操作动作编码',
    `from_status` VARCHAR(24) NULL COMMENT '操作前订单状态，创建订单时为空',
    `to_status` VARCHAR(24) NOT NULL COMMENT '操作后订单状态',
    `reason` VARCHAR(255) NULL COMMENT '操作原因',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_operate_log_order` (`order_id`, `created_at`, `id`),
    KEY `idx_order_operate_log_operator`
        (`operator_type`, `operator_id`, `created_at`, `id`),
    CONSTRAINT `fk_order_operate_log_order`
        FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_order_operate_log_operator_type`
        CHECK (`operator_type` IN ('USER', 'ADMIN', 'SYSTEM')),
    CONSTRAINT `chk_order_operate_log_operator_id` CHECK (
        (`operator_type` = 'SYSTEM' AND `operator_id` IS NULL)
        OR (`operator_type` IN ('USER', 'ADMIN') AND `operator_id` IS NOT NULL)
    ),
    CONSTRAINT `chk_order_operate_log_status_change` CHECK (
        (`operation` = 'CREATE' AND `from_status` IS NULL)
        OR (`operation` <> 'CREATE' AND `from_status` IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单操作日志表';


-- 创建outbox_event本地消息表
CREATE TABLE IF NOT EXISTS `outbox_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '事件主键 ID',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件全局唯一 ID',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
    `aggregate_type` VARCHAR(64) NOT NULL COMMENT '聚合根类型，如 ORDER',
    `aggregate_id` VARCHAR(64) NOT NULL COMMENT '聚合根业务主键，如 orderNo',
    `payload` TEXT NOT NULL COMMENT '事件消息体快照 (JSON 格式)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '发布状态：PENDING-待发布，PUBLISHED-已发布，FAILED-已失败',
    `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at` DATETIME(3) NULL COMMENT '下次重试时间',
    `trace_id` VARCHAR(64) NULL COMMENT '链路追踪 ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbox_event_id` (`event_id`),
    KEY `idx_outbox_status_retry` (`status`, `next_retry_at`, `created_at`),
    KEY `idx_outbox_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '本地消息表（Outbox Event）';

-- 创建event_consumption_log事件消费幂等记录表
CREATE TABLE IF NOT EXISTS `event_consumption_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消费主键 ID',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件全局唯一 ID',
    `consumer_name` VARCHAR(64) NOT NULL COMMENT '消费者标识',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
    `status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '消费状态：SUCCESS, FAILED',
    `detail` VARCHAR(512) NULL COMMENT '消费备注或异常信息',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '消费时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_consumer` (`event_id`, `consumer_name`),
    KEY `idx_consumer_created_at` (`consumer_name`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '事件消费幂等记录表';

-- 默认管理员：admin
-- 初始密码：Admin@123456（首次登录后应修改）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`)
SELECT
    'admin',
    '$2a$10$b8HYYC8WeV4oA629pnQSuuRuVkG/BOAWBGx0B6mi82Hwu/qIbGwd6',
    '系统管理员',
    'ADMIN',
    'ENABLED'
WHERE NOT EXISTS (
    SELECT 1
    FROM `user`
    WHERE `username` = 'admin'
);

SET FOREIGN_KEY_CHECKS = 1;
