-- 需求166：为已有订单表增加发货信息字段
ALTER TABLE `orders`
    ADD COLUMN `shipping_company` VARCHAR(64) NULL COMMENT '物流公司' AFTER `paid_at`,
    ADD COLUMN `tracking_number` VARCHAR(64) NULL COMMENT '物流单号' AFTER `shipping_company`;

-- P6：增加订单持久幂等键和库存操作 fencing token
ALTER TABLE `orders`
    ADD COLUMN `idempotency_token` VARCHAR(64) NULL COMMENT '用户维度的下单幂等 token' AFTER `user_id`,
    ADD UNIQUE KEY `uk_orders_user_idempotency` (`user_id`, `idempotency_token`);

ALTER TABLE `stock_operation_log`
    ADD COLUMN `owner_token` VARCHAR(64) NULL COMMENT '当前处理者的 fencing token' AFTER `status`;
