# order_item 商品快照表设计

## 设计目标

`order_item` 保存用户下单成功瞬间的商品信息。订单列表、订单详情、退款和库存恢复
都应读取该表中的快照，不应通过 `product_id` 回查并展示商品表中的当前名称、图片或
价格。商品后续改名、换图、调价、下架或逻辑删除都不能改变历史订单内容。

一笔订单包含多条商品快照，每种商品在同一订单中最多保存一条，购买数量记录在
`quantity` 中。

## 字段

| 字段 | 类型 | 是否为空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 商品快照主键 |
| `order_id` | `BIGINT UNSIGNED` | 否 | 无 | 所属订单 ID，关联 `orders.id` |
| `product_id` | `BIGINT UNSIGNED` | 否 | 无 | 下单时的商品 ID，仅作为历史业务标识 |
| `product_name` | `VARCHAR(128)` | 否 | 无 | 下单时的商品名称快照 |
| `product_image_url` | `VARCHAR(512)` | 是 | `NULL` | 下单时的商品主图 URL 快照 |
| `unit_price` | `DECIMAL(12, 2)` | 否 | 无 | 下单时数据库中的商品单价 |
| `quantity` | `INT UNSIGNED` | 否 | 无 | 购买数量，范围 1～99 |
| `subtotal_amount` | `DECIMAL(12, 2)` | 否 | 无 | 单项小计，等于单价乘以数量 |
| `created_at` | `DATETIME(3)` | 否 | `CURRENT_TIMESTAMP(3)` | 快照创建时间 |

商品快照创建后不允许修改，因此不设计 `updated_at` 和逻辑删除字段。

## 约束与索引

| 名称 | 类型 | 字段 | 用途 |
| --- | --- | --- | --- |
| `PRIMARY` | 主键 | `id` | 商品快照内部标识 |
| `uk_order_item_order_product` | 唯一索引 | `order_id, product_id` | 防止同一订单重复写入同一商品 |
| `idx_order_item_order` | 普通索引 | `order_id, id` | 按订单稳定读取全部商品快照 |
| `fk_order_item_order` | 外键 | `order_id -> orders.id` | 保证商品快照必须属于已存在的订单 |
| `chk_order_item_unit_price` | 检查约束 | `unit_price >= 0` | 禁止负数单价 |
| `chk_order_item_quantity` | 检查约束 | `quantity BETWEEN 1 AND 99` | 保证购买数量合法 |
| `chk_order_item_subtotal` | 检查约束 | `subtotal_amount = unit_price * quantity` | 保证单项小计与单价、数量一致 |

`product_id` 不建立到 `product` 表的外键。它只用于审计、库存恢复和业务追踪，避免
商品被物理清理时影响历史订单快照。业务代码也不能依赖该字段关联商品表来组装订单
展示数据。

## 快照写入规则

- `product_id`、`product_name`、`product_image_url` 和 `unit_price` 必须来自下单
  事务中读取到的数据库最新商品记录，不能取自 Redis 购物车中的旧数据或前端参数。
- `subtotal_amount` 使用 `BigDecimal` 按 `unit_price × quantity` 精确计算。
- 一笔订单的 `orders.total_amount` 必须等于其所有 `order_item.subtotal_amount` 之和。
- 主订单、全部商品快照和库存扣减必须处于同一个数据库事务中；任何一步失败都不能
  留下不完整快照。
