# orders 主表设计

## 设计目标

`orders` 保存订单级别的信息。商品和收货地址使用独立快照表保存，
不在主表中重复存储。订单需要保留完整业务记录，因此不设计逻辑删除字段。

## 字段

| 字段 | 类型 | 是否为空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键，仅供数据库内部关联 |
| `order_no` | `VARCHAR(32)` | 否 | 无 | 对外订单号，全局唯一且创建后不可修改 |
| `user_id` | `BIGINT UNSIGNED` | 否 | 无 | 下单用户 ID，关联 `user.id` |
| `total_amount` | `DECIMAL(12, 2)` | 否 | 无 | 按下单时数据库最新价格计算的订单总金额 |
| `status` | `VARCHAR(24)` | 否 | `PENDING_PAYMENT` | 订单当前状态 |
| `expire_at` | `DATETIME(3)` | 否 | 无 | 待付款截止时间，用于超时取消 |
| `paid_at` | `DATETIME(3)` | 是 | `NULL` | 支付成功时间 |
| `shipped_at` | `DATETIME(3)` | 是 | `NULL` | 管理员发货时间 |
| `completed_at` | `DATETIME(3)` | 是 | `NULL` | 用户确认收货时间 |
| `cancelled_at` | `DATETIME(3)` | 是 | `NULL` | 用户主动取消或系统超时取消时间 |
| `created_at` | `DATETIME(3)` | 否 | `CURRENT_TIMESTAMP(3)` | 订单创建时间 |
| `updated_at` | `DATETIME(3)` | 否 | `CURRENT_TIMESTAMP(3)` | 最后更新时间，更新记录时自动刷新 |

金额统一使用 `DECIMAL(12, 2)`，禁止使用浮点类型。`total_amount` 必须大于或
等于 0，并且只能由后端根据商品快照计算，不能采用前端传入的金额。

状态值由后续订单状态枚举任务统一落地。主流程需要覆盖待付款、待发货、已发货、
已完成和已取消五种状态；数据库字段长度为后续枚举值预留空间。

## 约束与索引

| 名称 | 类型 | 字段 | 用途 |
| --- | --- | --- | --- |
| `PRIMARY` | 主键 | `id` | 订单内部关联 |
| `uk_orders_order_no` | 唯一索引 | `order_no` | 保证订单号全局唯一，并支持按订单号查询 |
| `idx_orders_user_created` | 普通索引 | `user_id, created_at, id` | 用户全部订单倒序分页 |
| `idx_orders_user_status_created` | 普通索引 | `user_id, status, created_at, id` | 用户按状态筛选并倒序分页 |
| `idx_orders_status_expire` | 普通索引 | `status, expire_at, id` | 分页扫描已过期的待付款订单 |
| `fk_orders_user` | 外键 | `user_id -> user.id` | 保证订单所属用户存在，限制用户被物理删除 |
| `chk_orders_total_amount` | 检查约束 | `total_amount >= 0` | 防止保存负数金额 |

所有分页索引最后包含 `id`，用于在时间相同时提供稳定排序。订单号采用唯一索引，
即使应用层订单号生成发生碰撞，数据库仍会拒绝重复订单。

## 时间字段写入规则

- 创建订单时写入 `created_at`、`updated_at` 和 `expire_at`，其余业务时间为空。
- 支付成功时写入 `paid_at`。
- 发货成功时写入 `shipped_at`。
- 确认收货时写入 `completed_at`。
- 用户取消或系统超时取消时写入 `cancelled_at`。
- 每次状态变化都同步更新 `updated_at`。

业务时间只能在对应状态转换成功后写入，状态更新失败时不得单独更新时间字段。
