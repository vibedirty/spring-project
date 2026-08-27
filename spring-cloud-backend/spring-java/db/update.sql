-- 需求166：为已有订单表增加发货信息字段
ALTER TABLE `orders`
    ADD COLUMN `shipping_company` VARCHAR(64) NULL COMMENT '物流公司' AFTER `paid_at`,
    ADD COLUMN `tracking_number` VARCHAR(64) NULL COMMENT '物流单号' AFTER `shipping_company`;
