# 金额计算与序列化精度审查记录

- 审查日期：2026-08-26
- 审查范围：商品价格输入、购物车金额、订单金额计算、实体与 DTO、数据库字段及 JSON 序列化
- 审查目标：金额计算不使用 `double` 或 `float`，存储和传输过程不丢失十进制精度

## 审查结论

金额全链路使用 `BigDecimal`，数据库金额字段使用 `DECIMAL(12, 2)`，JSON 将 `BigDecimal` 序列化为字符串。未发现使用 `double`、`Double`、`float` 或 `Float` 参与金额计算的代码，生产逻辑无需修改。

代码中存在的 `Double.NEGATIVE_INFINITY` 和 `double toScore(...)` 仅用于 Redis 有序集合中的订单超时时间分值，不表示金额，也不参与价格或订单总额计算。

## 输入精度

- `ProductCreateRequest.price` 和 `ProductUpdateRequest.price` 类型均为 `BigDecimal`。
- `@Digits(integer = 10, fraction = 2)` 限制最多 10 位整数和 2 位小数。
- `@DecimalMin("0.00")` 禁止负数价格。
- JSON 小数直接反序列化为 `BigDecimal`，没有经过二进制浮点类型中转。

## 计算精度

| 场景 | 实现 | 结论 |
| --- | --- | --- |
| 购物车选中金额 | `BigDecimal.add`、`BigDecimal.multiply` | 精确十进制计算 |
| 订单商品小计 | 商品单价乘以整数数量 | 不产生额外小数位 |
| 订单总金额 | 从 `BigDecimal.ZERO` 开始逐项相加 | 不使用浮点累加 |
| 订单价格快照 | `unitPrice` 和 `subtotalAmount` 均为 `BigDecimal` | 下单后不受商品价格变化影响 |

商品价格最多 2 位小数，数量为整数，因此乘法结果仍最多 2 位小数，无须通过浮点舍入修正。

## 数据库存储

| 字段 | 数据库类型 | 约束 |
| --- | --- | --- |
| `product.price` | `DECIMAL(12, 2)` | 非负 |
| `orders.total_amount` | `DECIMAL(12, 2)` | 非负 |
| `order_item.unit_price` | `DECIMAL(12, 2)` | 非负 |
| `order_item.subtotal_amount` | `DECIMAL(12, 2)` | 等于单价乘数量 |

MyBatis-Plus 将上述字段映射为 Java `BigDecimal`，数据库读写过程中不会转换为二进制浮点数。订单主表、订单明细和库存扣减处于同一事务，金额越界或数据库约束失败时不会留下部分订单数据。

## JSON 序列化

- `JacksonConfiguration.bigDecimalToStringJsonMapperCustomizer()` 为所有 `BigDecimal` 注册 `ToStringSerializer`。
- 金额以 JSON 字符串输出，例如 `"0.10"`、`"0.30"` 和 `"45.29"`。
- 字符串输出既保留十进制值和小数位，也避免 JavaScript 将金额转换为 IEEE 754 浮点数后产生误差。
- 商品、购物车、订单列表、订单详情和管理端订单响应中的金额字段都会使用该全局规则。

## 测试证据

- `OrderAmountCalculatorTests.shouldCalculateEachSubtotalAndTotalPrecisely` 验证 `0.10 × 3 = 0.30`，总金额精确为 `45.29`。
- `OrderAmountCalculatorTests.shouldKeepCurrentUnitPriceAndQuantityInResult` 验证订单商品小计精度。
- `JacksonConfigurationTests.shouldSerializeBigDecimalAmountsAsExactStrings` 验证金额以保留小数位的字符串输出。
- `ProductCreateRequestTests` 和 `ProductUpdateRequestTests` 验证整数位、小数位和非负约束。
- 数据库建表脚本通过 `CHECK` 约束验证订单商品小计关系。

## 后续约束

- 新增金额字段必须使用 `BigDecimal` 和数据库 `DECIMAL`，禁止使用 `double` 或 `float`。
- 禁止使用 `new BigDecimal(doubleValue)`；常量应使用字符串构造或精确的 `BigDecimal` 常量。
- 除非业务明确引入折扣、税率或分摊，否则不要增加不必要的舍入。
- 如果引入除法，必须明确指定小数位和 `RoundingMode`，并为边界金额增加测试。
- 新增金额响应必须继续使用全局 `BigDecimal` 字符串序列化规则。
