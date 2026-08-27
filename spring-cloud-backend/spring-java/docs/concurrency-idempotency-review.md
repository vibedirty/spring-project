# 并发与幂等路径审查记录

- 审查日期：2026-08-26
- 审查范围：库存扣减与恢复、订单创建、付款、取消、自动取消、发货和提交后副作用
- 审查目标：并发请求或重复请求不会导致库存超卖、重复付款、重复恢复库存、重复发货或重复操作日志

## 审查结论

库存、付款、取消和发货均以数据库条件更新作为最终一致性保障，只有成功改变状态的请求才执行后续数据库副作用。取消流程额外使用订单维度的 Redis 分布式锁，订单创建使用用户与幂等 token 组合的 Redis 占位。审查未发现重复副作用路径，生产逻辑无需修改。

分布式锁不是唯一正确性条件：即使锁失效或请求落到不同实例，数据库中的库存条件和订单状态条件仍会保证最多一个请求成功改变数据。

## 库存

- 增加库存使用数据库原子自增，并限制结果不超过 `Integer.MAX_VALUE`。
- 减少库存使用数据库原子自减，SQL 条件要求当前库存不少于扣减数量，避免并发超卖和负库存。
- 创建订单的多个商品扣减在同一事务中执行；任意商品不足时，之前的扣减和库存日志全部回滚。
- 取消订单的库存恢复与订单状态改变在同一事务中执行；恢复失败时订单状态和已恢复库存全部回滚。
- 订单取消只有从 `PENDING_PAYMENT` 成功变为 `CANCELLED` 的请求才会恢复库存，因此重复取消不会重复增加库存。
- 商品缓存只在事务提交后清理，回滚不会产生错误的缓存失效时序。

后台人工库存调整的每次调用代表一次独立业务操作；订单驱动的扣减和恢复由订单创建幂等与订单状态机控制重复副作用。

## 订单创建幂等

- 客户端提供幂等 token 时，Redis key 使用 `order:idempotency:{userId}:{token}`。
- `setIfAbsent` 保证同一用户、同一 token 在有效期内只有一个请求进入订单创建流程。
- Redis value 使用随机所有权标识；释放时通过 Lua 脚本比较 value 后删除，旧请求不能删除新请求的占位。
- 创建失败且数据库事务已经回滚后释放 token，允许客户端安全重试。
- 创建成功后保留 token 至 TTL 到期，重复提交会返回业务冲突。

## 付款

- 付款更新条件同时包含订单号、用户 ID、`PENDING_PAYMENT` 状态和未过期条件。
- 并发付款时只有一个 SQL 更新返回 1，只有该请求写入付款操作日志并登记提交后副作用。
- 后续相同付款请求看到 `PENDING_SHIPMENT + paidAt` 后幂等返回，不修改首次付款时间，也不重复写日志。
- 付款与取消竞争同一个 `PENDING_PAYMENT` 前置状态，数据库行条件保证二者只能有一个成功。

## 取消与自动取消

- 用户取消和自动取消均通过 `OrderLockService.executeWithStatusLock` 获取订单号维度的 Redis 锁。
- 数据库更新再次要求订单状态为 `PENDING_PAYMENT`；自动取消还要求 `expire_at` 已到期。
- 只有成功更新为 `CANCELLED` 的请求才恢复库存、写取消日志和删除 Redis 超时任务。
- 重复取消返回 `false`，不会再次恢复库存或写日志。
- 订单状态、库存恢复、库存日志和订单操作日志在同一数据库事务中提交或回滚。

## 发货

- 发货 SQL 只允许 `PENDING_SHIPMENT` 原子更新为 `SHIPPED`。
- 只有更新成功的请求写一条发货操作日志。
- 使用相同物流公司和运单号重复请求时返回首次发货结果，不修改发货时间，也不重复写日志。
- 已发货订单使用不同物流信息再次请求时返回业务冲突，不能覆盖首次发货信息。
- 并发发货时数据库行更新保证只有一个请求能从 `PENDING_SHIPMENT` 改变状态；其他请求进入幂等或冲突分支。

## 提交后副作用

- 商品缓存清理、购物车清理、订单超时任务增删和业务成功日志均通过提交后回调执行。
- 数据库事务回滚时不会执行成功副作用。
- Redis 缓存或超时任务操作失败会记录告警，不会反向破坏已经提交的订单事务。

## 测试证据

- `StockOrderDeductionTests.shouldRollbackEarlierDeductionsWhenAnyProductFails`
- `StockOrderRestorationTests.shouldRollbackEarlierRestorationsWhenAnyProductOverflows`
- `OrderCreationTransactionTests.shouldRollbackAllDatabaseChangesWhenAnyStepFails`
- `OrderIdempotencyRedisTests.shouldRejectDuplicateTokenAndAllowRetryAfterOwnedRelease`
- `OrderPaymentServiceTests.shouldOnlyPayPendingPaymentOrderOnce`
- `OrderPaymentServiceTests.shouldOnlyPayOnceForConcurrentPayments`
- `OrderCancellationServiceTests.shouldCancelAndRestoreStockOnlyOnce`
- `OrderCancellationServiceTests.shouldOnlyRestoreStockOnceForConcurrentCancellations`
- `AdminOrderServiceTests.shouldReturnIdempotentlyWithoutDuplicateLogForRepeatedShipment`
- `AdminOrderServiceTests.shouldRejectRepeatedShipmentWithDifferentShipmentInformation`
- `OrderReceiptServiceTests.shouldReturnIdempotentlyWithoutDuplicateLogForRepeatedConfirmation`

## 后续约束

- 状态流转必须继续使用带原状态条件的单条 SQL 更新，禁止“先查询状态再无条件更新”。
- 业务副作用只能由状态更新成功的请求执行，失败和幂等分支不得重复写日志、库存或物流信息。
- Redis 锁只能作为减少竞争的第一层保护，数据库条件更新必须保留。
- 新增可重试写接口时应定义业务幂等键，并保证键包含用户或业务主体维度。
- 新增订单状态后必须补充重复请求、并发请求以及相邻状态竞争测试。
