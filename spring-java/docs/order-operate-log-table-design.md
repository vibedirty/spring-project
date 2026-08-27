# order_operate_log 订单操作日志表设计

## 设计目标

`order_operate_log` 以追加方式记录订单生命周期中的关键操作，用于审计状态变化和
排查异常。日志需要区分用户、管理员和系统自动任务，并同时保存操作前状态、操作后
状态、操作动作、原因和发生时间。

日志一旦写入不允许更新或删除。订单当前状态仍以 `orders.status` 为准，操作日志仅
记录历史过程，不能反向作为订单当前状态的数据源。

## 字段

| 字段 | 类型 | 是否为空 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 操作日志主键 |
| `order_id` | `BIGINT UNSIGNED` | 否 | 无 | 所属订单 ID，关联 `orders.id` |
| `operator_type` | `VARCHAR(16)` | 否 | 无 | 操作者类型：`USER`、`ADMIN`、`SYSTEM` |
| `operator_id` | `BIGINT UNSIGNED` | 是 | `NULL` | 用户或管理员 ID；系统操作时为空 |
| `operator_name` | `VARCHAR(64)` | 否 | 无 | 操作者名称快照；系统操作统一写 `SYSTEM` |
| `operation` | `VARCHAR(32)` | 否 | 无 | 操作动作，如创建、支付、取消、发货、确认收货 |
| `from_status` | `VARCHAR(24)` | 是 | `NULL` | 操作前订单状态；创建订单时为空 |
| `to_status` | `VARCHAR(24)` | 否 | 无 | 操作成功后的订单状态 |
| `reason` | `VARCHAR(255)` | 是 | `NULL` | 主动取消、超时取消或其他操作原因 |
| `created_at` | `DATETIME(3)` | 否 | `CURRENT_TIMESTAMP(3)` | 操作成功时间 |

`operator_name` 是操作发生时的名称快照，后续用户昵称或管理员信息变化不能改写历史
日志。日志只记录已经成功完成的业务操作，失败尝试由应用日志记录，不写入该表。

## 操作者与动作约定

| `operator_type` | `operator_id` | `operator_name` | 典型动作 |
| --- | --- | --- | --- |
| `USER` | 当前用户 ID | 当前用户名称快照 | 创建订单、支付、主动取消、确认收货 |
| `ADMIN` | 当前管理员 ID | 当前管理员名称快照 | 发货、后台业务处理 |
| `SYSTEM` | `NULL` | `SYSTEM` | 订单超时自动取消、补偿任务处理 |

`operation` 使用稳定的英文业务编码，初步覆盖：

- `CREATE`：创建待付款订单。
- `PAY`：支付成功。
- `CANCEL`：用户主动取消。
- `AUTO_CANCEL`：系统超时取消。
- `SHIP`：管理员发货。
- `CONFIRM_RECEIPT`：用户确认收货。

订单状态枚举和合法转换关系由后续状态机任务统一定义，日志表只负责保存转换结果。

## 约束与索引

| 名称 | 类型 | 字段 | 用途 |
| --- | --- | --- | --- |
| `PRIMARY` | 主键 | `id` | 日志内部标识 |
| `idx_order_operate_log_order` | 普通索引 | `order_id, created_at, id` | 按时间稳定读取订单完整操作历史 |
| `idx_order_operate_log_operator` | 普通索引 | `operator_type, operator_id, created_at, id` | 按操作者追踪用户或管理员操作 |
| `fk_order_operate_log_order` | 外键 | `order_id -> orders.id` | 保证日志必须属于已存在的订单 |
| `chk_order_operate_log_operator_type` | 检查约束 | `operator_type IN ('USER', 'ADMIN', 'SYSTEM')` | 限制操作者类型 |
| `chk_order_operate_log_operator_id` | 检查约束 | 系统操作 ID 为空，用户和管理员操作 ID 非空 | 保证操作者字段组合一致 |
| `chk_order_operate_log_status_change` | 检查约束 | 创建操作前状态为空，其他操作前状态非空 | 保证状态变化记录完整 |

`operator_id` 不建立到 `user` 表的外键，避免账号后续处理影响审计日志的长期保留；
操作者身份通过 ID、类型和名称快照共同记录。

## 写入规则

- 创建订单时写入首条 `CREATE` 日志，`from_status` 为空，`to_status` 为待付款。
- 每次状态转换必须在更新订单状态成功后写日志，`from_status` 和 `to_status` 必须与
  本次条件状态更新一致。
- 订单状态更新与日志插入必须在同一个数据库事务中，任一失败时同时回滚。
- 重复支付、重复取消、重复发货或重复确认收货等幂等请求没有产生新状态转换时，
  不重复写入操作日志。
- 主动取消和系统超时取消必须写明 `reason`，以便区分取消来源和追踪库存恢复。
