-- 需求166：为已有订单表增加发货信息字段
ALTER TABLE `orders`
    ADD COLUMN `shipping_company` VARCHAR(64) NULL COMMENT '物流公司' AFTER `paid_at`,
    ADD COLUMN `tracking_number` VARCHAR(64) NULL COMMENT '物流单号' AFTER `shipping_company`;

-- P6：创建outbox_event本地消息表
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

-- P6：创建event_consumption_log事件消费幂等记录表
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

