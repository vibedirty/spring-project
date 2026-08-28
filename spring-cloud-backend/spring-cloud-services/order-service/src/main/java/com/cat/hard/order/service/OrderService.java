package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.auth.security.CurrentUser;
import com.cat.hard.order.calculator.OrderAmountCalculator;
import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.common.exception.BusinessException;
import com.cat.hard.order.common.util.TextUtils;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.dto.OrderDetailResponse;
import com.cat.hard.order.dto.OrderListRequest;
import com.cat.hard.order.dto.OrderListResponse;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.generator.OrderNumberGenerator;
import com.cat.hard.order.integration.account.dto.AddressSnapshot;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.integration.account.service.AccountQueryService;
import com.cat.hard.order.integration.cart.dto.CartItemResponse;
import com.cat.hard.order.integration.cart.service.CartQueryService;
import com.cat.hard.order.integration.product.dto.StockDeductionItem;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.dto.StockRestorationItem;
import com.cat.hard.order.integration.product.enums.ProductStatus;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.model.OrderAmountResult;
import com.cat.hard.order.model.OrderIdempotencyLock;
import com.cat.hard.order.model.OrderItemAmount;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final long PAYMENT_TIMEOUT_MINUTES = 5L;

	@Resource
	private CartQueryService cartQueryService;

	@Resource
	private OrderAmountCalculator orderAmountCalculator;

	@Resource
	private CurrentUser currentUser;

	@Resource
	private OrderNumberGenerator orderNumberGenerator;

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private OrderAddressMapper orderAddressMapper;

	@Resource
	private AccountQueryService accountQueryService;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private ProductStockIntegrationService productStockIntegrationService;

	@Resource
	private OrderIdempotencyService orderIdempotencyService;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderCancellationTransactionService orderCancellationTransactionService;

	@Resource
	private OrderSagaTransactionService orderSagaTransactionService;

	@Resource
	private TransactionTemplate transactionTemplate;

	public Order createOrder(OrderCreateRequest request) {
		Long userId = currentUser.getUserId();
		String idempotencyToken = TextUtils.trimToNull(request.getIdempotencyToken());
		if (idempotencyToken == null) {
			throw new BusinessException(ErrorCode.PARAMETER_ERROR, "幂等token不能为空");
		}
		Order existingOrder = orderMapper.selectByUserIdAndIdempotencyToken(userId, idempotencyToken);
		if (existingOrder != null) {
			return existingOrder;
		}
		OrderIdempotencyLock idempotencyLock = orderIdempotencyService.acquire(
				userId,
				idempotencyToken);

		try {
			existingOrder = orderMapper.selectByUserIdAndIdempotencyToken(userId, idempotencyToken);
			if (existingOrder != null) {
				return existingOrder;
			}
			List<CartItemResponse> selectedItems = getValidatedSelectedCartItems();
			OrderAmountResult amountResult = orderAmountCalculator.calculate(selectedItems);
			AddressSnapshot addressSnapshot = accountQueryService.getAddressSnapshot(
					userId,
					request.getAddressId());
			if (addressSnapshot == null) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收货地址不存在");
			}
			UserSummary userSummary = accountQueryService.getUserSummary(userId);

			// Saga Phase 1: 本地事务插入初始订单 (PENDING_STOCK)
			Order order;
			try {
				order = transactionTemplate.execute(status -> createPendingStockOrderInTransaction(
						selectedItems,
						amountResult,
						addressSnapshot,
						userSummary,
						idempotencyToken));
			}
			catch (DuplicateKeyException exception) {
				Order duplicate = orderMapper.selectByUserIdAndIdempotencyToken(userId, idempotencyToken);
				if (duplicate != null) {
					return duplicate;
				}
				throw exception;
			}

			if (order == null) {
				throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "创建订单初始状态失败");
			}

			// Saga Phase 2: 远程调用扣减库存
			List<StockDeductionItem> deductionItems = new ArrayList<>();
			for (CartItemResponse item : selectedItems) {
				deductionItems.add(new StockDeductionItem(
						item.getProductId(),
						item.getProductName(),
						item.getQuantity()));
			}

			try {
				productStockIntegrationService.decreaseForOrder(order.getOrderNo(), deductionItems);
				return promoteAfterStockDeduct(order, deductionItems);
			}
			catch (Exception e) {
				// 远程超时或网络异常，尝试查询库存扣减结果
				log.warn("订单{}扣减库存异常，尝试回查处理结果: {}", order.getOrderNo(), e.getMessage());
				StockOperationResultResponse result = null;
				try {
					result = productStockIntegrationService.queryStockResult(order.getOrderNo());
				}
				catch (Exception ex) {
					log.warn("回查库存结果失败: {}", ex.getMessage());
				}

				final StockOperationResultResponse finalResult = result;
				if (isOperation(finalResult, "DEDUCT", "SUCCESS")) {
					return promoteAfterStockDeduct(order, deductionItems);
				}
				else if (isOperation(finalResult, "DEDUCT", "FAILED")) {
					transactionTemplate.execute(status -> {
						markOrderCancelled(order.getOrderNo(), "扣减库存失败：" + finalResult.detail());
						return null;
					});
					if (e instanceof BusinessException businessException) {
						throw businessException;
					}
					throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库存扣减失败，订单已取消");
				}
				else if (isOperation(finalResult, "RESTORE", "SUCCESS")) {
					transactionTemplate.execute(status -> {
						markOrderCancelled(order.getOrderNo(), "库存已恢复，订单创建终止");
						return null;
					});
					throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "订单创建已终止");
				}
				else {
					// 无法确认状态，保持 PENDING_STOCK，由后台 Saga 补偿任务核对
					log.warn("订单{}扣减库存状态未知，留给 Saga 补偿任务处理", order.getOrderNo());
					throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "库存处理中，请稍后查看订单状态");
				}
			}
		}
		finally {
			releaseIdempotencyLock(idempotencyLock);
		}
	}

	private Order createPendingStockOrderInTransaction(
			List<CartItemResponse> selectedItems,
			OrderAmountResult amountResult,
			AddressSnapshot addressSnapshot,
			UserSummary userSummary,
			String idempotencyToken) {

		Order order = new Order();
		order.setOrderNo(orderNumberGenerator.generate());
		order.setUserId(currentUser.getUserId());
		order.setIdempotencyToken(idempotencyToken);
		order.setTotalAmount(amountResult.getTotalAmount());
		order.setStatus(OrderStatus.PENDING_STOCK);
		order.setExpireAt(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES));
		orderMapper.insert(order);

		createOrderItems(order.getId(), selectedItems, amountResult);
		createOrderAddress(order.getId(), addressSnapshot);
		createOrderCreateLog(order, userSummary);
		return order;
	}

	private Order promoteAfterStockDeduct(
			Order order,
			List<StockDeductionItem> deductionItems) {
		Order promoted = orderSagaTransactionService.promoteToPendingPayment(order.getOrderNo());
		if (promoted != null && promoted.getStatus() == OrderStatus.PENDING_PAYMENT) {
			return promoted;
		}

		List<StockRestorationItem> restorationItems = deductionItems.stream()
				.map(item -> new StockRestorationItem(
						item.getProductId(),
						item.getProductName(),
						item.getQuantity()))
				.toList();
		try {
			productStockIntegrationService.restoreForOrder(order.getOrderNo(), restorationItems);
		}
		catch (RuntimeException exception) {
			log.error("订单{}状态晋级失败后的库存恢复也失败，需要人工对账", order.getOrderNo(), exception);
		}
		throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "订单状态已变化，创建流程已终止");
	}

	private boolean isOperation(
			StockOperationResultResponse result,
			String operationType,
			String status) {
		return result != null
				&& operationType.equalsIgnoreCase(result.operationType())
				&& status.equalsIgnoreCase(result.status());
	}

	public void markOrderCancelled(String orderNo, String reason) {
		LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(Order::getOrderNo, orderNo)
				.eq(Order::getStatus, OrderStatus.PENDING_STOCK)
				.set(Order::getStatus, OrderStatus.CANCELLED)
				.set(Order::getCancelledAt, LocalDateTime.now())
				.set(Order::getUpdatedAt, LocalDateTime.now());
		if (orderMapper.update(null, updateWrapper) != 1) {
			return;
		}

		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null) {
			OrderOperateLog operateLog = new OrderOperateLog();
			operateLog.setOrderId(order.getId());
			operateLog.setOperatorType(OrderOperatorType.SYSTEM);
			operateLog.setOperatorName("SYSTEM");
			operateLog.setOperation(OrderOperation.CANCEL);
			operateLog.setFromStatus(OrderStatus.PENDING_STOCK);
			operateLog.setToStatus(OrderStatus.CANCELLED);
			operateLog.setReason(reason);
			orderOperateLogMapper.insert(operateLog);
		}
	}

	@Transactional(readOnly = true)
	public Page<OrderListResponse> pageMyOrders(OrderListRequest request) {
		LambdaQueryWrapper<Order> orderQuery =
				new LambdaQueryWrapper<Order>(Order.class);
		orderQuery.eq(Order::getUserId, currentUser.getUserId());
		if (request.getStatus() != null) {
			orderQuery.eq(Order::getStatus, request.getStatus());
		}
		orderQuery.orderByDesc(Order::getCreatedAt)
				.orderByDesc(Order::getId);

		Page<Order> orderPage = orderMapper.selectPage(
				request.toPage(),
				orderQuery);
		List<OrderListResponse> responses = buildOrderListResponses(
				orderPage.getRecords());

		Page<OrderListResponse> responsePage = new Page<OrderListResponse>(
				orderPage.getCurrent(),
				orderPage.getSize(),
				orderPage.getTotal());
		responsePage.setRecords(responses);
		return responsePage;
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getMyOrderDetail(String orderNo) {
		LambdaQueryWrapper<Order> orderQuery =
				new LambdaQueryWrapper<Order>(Order.class);
		orderQuery.eq(Order::getOrderNo, orderNo)
				.eq(Order::getUserId, currentUser.getUserId());
		Order order = orderMapper.selectOne(orderQuery);
		if (order == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"订单不存在");
		}

		List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
		OrderAddress orderAddress = orderAddressMapper.selectByOrderId(order.getId());
		List<OrderOperateLog> operateLogs =
				orderOperateLogMapper.selectByOrderId(order.getId());
		return OrderDetailResponse.from(
				order,
				orderItems,
				orderAddress,
				operateLogs);
	}

	public boolean cancelOrder(String orderNo) {
		Long userId = currentUser.getUserId();
		UserSummary userSummary = accountQueryService.getUserSummary(userId);
		return orderLockService.executeWithStatusLock(
				orderNo,
				() -> orderCancellationTransactionService.cancel(
						orderNo,
						userId,
						userSummary));
	}

	private List<OrderListResponse> buildOrderListResponses(List<Order> orders) {
		if (orders.isEmpty()) {
			return List.of();
		}

		List<Long> orderIds = new ArrayList<>();
		for (Order order : orders) {
			orderIds.add(order.getId());
		}

		LambdaQueryWrapper<OrderItem> itemQuery =
				new LambdaQueryWrapper<OrderItem>(OrderItem.class);
		itemQuery.in(OrderItem::getOrderId, orderIds)
				.orderByAsc(OrderItem::getId);
		List<OrderItem> orderItems = orderItemMapper.selectList(itemQuery);

		Map<Long, List<OrderItem>> itemsByOrderId = new HashMap<>();
		for (OrderItem orderItem : orderItems) {
			itemsByOrderId.computeIfAbsent(
					orderItem.getOrderId(),
					key -> new ArrayList<>())
					.add(orderItem);
		}

		List<OrderListResponse> responses = new ArrayList<>();
		for (Order order : orders) {
			List<OrderItem> items = itemsByOrderId.getOrDefault(
					order.getId(),
					List.of());
			responses.add(OrderListResponse.from(order, items));
		}
		return responses;
	}

	private void releaseIdempotencyLock(
			OrderIdempotencyLock idempotencyLock) {
		if (idempotencyLock == null) {
			return;
		}

		try {
			orderIdempotencyService.release(idempotencyLock);
		}
		catch (RuntimeException exception) {
			log.warn(
					"释放订单创建互斥锁失败，key={}",
					idempotencyLock.getKey(),
					exception);
		}
	}

	public List<CartItemResponse> getSelectedCartItems() {
		List<CartItemResponse> selectedItems = cartQueryService.getSelectedCartItems(currentUser.getUserId());
		if (selectedItems.isEmpty()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"购物车中没有选中的商品");
		}
		return selectedItems;
	}

	public List<CartItemResponse> getValidatedSelectedCartItems() {
		List<CartItemResponse> selectedItems = getSelectedCartItems();
		for (CartItemResponse item : selectedItems) {
			validateItem(item);
		}
		return selectedItems;
	}

	private List<OrderItem> createOrderItems(
			Long orderId,
			List<CartItemResponse> cartItems,
			OrderAmountResult amountResult) {

		Map<Long, OrderItemAmount> amountMap = new HashMap<>();
		for (OrderItemAmount itemAmount : amountResult.getItems()) {
			amountMap.put(itemAmount.getProductId(), itemAmount);
		}

		List<OrderItem> orderItems = new ArrayList<>();
		for (CartItemResponse cartItem : cartItems) {
			OrderItemAmount itemAmount = amountMap.get(cartItem.getProductId());
			if (itemAmount == null) {
				throw new IllegalStateException(
						"缺少商品金额计算结果，商品ID：" + cartItem.getProductId());
			}

			OrderItem orderItem = new OrderItem();
			orderItem.setOrderId(orderId);
			orderItem.setProductId(cartItem.getProductId());
			orderItem.setProductName(cartItem.getProductName());
			orderItem.setProductImageUrl(cartItem.getImageUrl());
			orderItem.setUnitPrice(itemAmount.getUnitPrice());
			orderItem.setQuantity(itemAmount.getQuantity());
			orderItem.setSubtotalAmount(itemAmount.getSubtotalAmount());
			orderItemMapper.insert(orderItem);
			orderItems.add(orderItem);
		}

		return orderItems;
	}

	private OrderAddress createOrderAddress(
			Long orderId,
			AddressSnapshot sourceAddress) {
		OrderAddress orderAddress = new OrderAddress();
		orderAddress.setOrderId(orderId);
		orderAddress.setSourceAddressId(sourceAddress.addressId());
		orderAddress.setReceiverName(sourceAddress.receiverName());
		orderAddress.setPhone(sourceAddress.phone());
		orderAddress.setProvince(sourceAddress.province());
		orderAddress.setCity(sourceAddress.city());
		orderAddress.setDistrict(sourceAddress.district());
		orderAddress.setDetailAddress(sourceAddress.detailAddress());
		orderAddressMapper.insert(orderAddress);
		return orderAddress;
	}

	private OrderOperateLog createOrderCreateLog(
			Order order,
			UserSummary userSummary) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(order.getUserId());
		operateLog.setOperatorName(userSummary != null ? userSummary.nickname() : "用户");
		operateLog.setOperation(OrderOperation.CREATE);
		operateLog.setFromStatus(null);
		operateLog.setToStatus(OrderStatus.PENDING_STOCK);
		operateLog.setReason("用户创建订单");
		orderOperateLogMapper.insert(operateLog);
		return operateLog;
	}

	private void validateItem(CartItemResponse item) {
		if (item.getProductStatus() == null) {
			throwInvalidItem(item, "商品不存在或已删除");
		}
		if (item.getProductStatus() != ProductStatus.ON_SALE) {
			throwInvalidItem(item, "商品已下架");
		}

		Integer quantity = item.getQuantity();
		if (quantity == null || quantity < 1 || quantity > 99) {
			throwInvalidItem(item, "购买数量必须在1到99之间");
		}
		if (item.getStock() == null || item.getStock() < quantity) {
			throwInvalidItem(item, "商品库存不足");
		}
		if (!Boolean.TRUE.equals(item.getValid())) {
			String reason = item.getInvalidReason();
			throwInvalidItem(item, reason == null ? "商品当前不可购买" : reason);
		}
	}

	private void throwInvalidItem(CartItemResponse item, String reason) {
		String productLabel;
		if (item.getProductName() == null || item.getProductName().isBlank()) {
			productLabel = "商品（ID：" + item.getProductId() + "）";
		} else {
			productLabel = "商品“" + item.getProductName()
					+ "”（ID：" + item.getProductId() + "）";
		}
		throw new BusinessException(
				ErrorCode.BUSINESS_CONFLICT,
				productLabel + "：" + reason);
	}
}
