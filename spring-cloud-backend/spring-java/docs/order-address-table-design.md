# order_address 地址快照表设计

## 设计目标

`order_address` 保存用户提交订单时选中的收货地址快照。订单创建后，用户修改默认
地址、编辑原地址或删除地址都不能改变历史订单的收货信息。

地址快照与订单是一对一关系：每笔订单必须且只能拥有一条地址快照。订单列表和详情
应直接读取快照内容，不得通过原地址 ID 回查 `user_address` 表来组装收货信息。

## 字段

| 字段 | 类型 | 是否为空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 地址快照主键 |
| `order_id` | `BIGINT UNSIGNED` | 否 | 无 | 所属订单 ID，关联 `orders.id` |
| `source_address_id` | `BIGINT UNSIGNED` | 否 | 无 | 下单时选中的用户地址 ID，仅用于审计追踪 |
| `receiver_name` | `VARCHAR(32)` | 否 | 无 | 下单时的收货人姓名 |
| `phone` | `VARCHAR(20)` | 否 | 无 | 下单时的收货人手机号 |
| `province` | `VARCHAR(64)` | 否 | 无 | 下单时的省或直辖市 |
| `city` | `VARCHAR(64)` | 否 | 无 | 下单时的市 |
| `district` | `VARCHAR(64)` | 否 | 无 | 下单时的区或县 |
| `detail_address` | `VARCHAR(255)` | 否 | 无 | 下单时的详细地址 |
| `created_at` | `DATETIME(3)` | 否 | `CURRENT_TIMESTAMP(3)` | 快照创建时间 |

字段类型和长度与 `user_address` 中对应字段保持一致，避免写入快照时发生截断。地址
快照创建后不允许修改，因此不设计 `updated_at`、`is_default` 和逻辑删除字段。

## 约束与索引

| 名称 | 类型 | 字段 | 用途 |
| --- | --- | --- | --- |
| `PRIMARY` | 主键 | `id` | 地址快照内部标识 |
| `uk_order_address_order` | 唯一索引 | `order_id` | 保证一笔订单只能有一条地址快照，并支持按订单查询 |
| `fk_order_address_order` | 外键 | `order_id -> orders.id` | 保证地址快照必须属于已存在的订单 |

`source_address_id` 不建立到 `user_address` 表的外键。原地址允许被用户逻辑删除，
快照只保存下单时的事实，不应依赖原地址记录继续存在。

## 快照写入规则

- 只能读取当前登录用户拥有且未删除的地址，不能为订单保存其他用户的地址。
- `source_address_id` 和全部收货信息必须来自同一次读取到的 `user_address` 记录，
  不能接受前端直接传入收货人、电话或详细地址。
- 必须逐字段复制收货人、电话、省、市、区县和详细地址，不能只保存原地址 ID。
- 主订单、商品快照和地址快照必须在同一个数据库事务中创建；地址校验或快照写入
  失败时，整笔订单创建都必须回滚。
- 后续发货和订单详情始终使用该快照，不能因用户修改地址而自动同步。
