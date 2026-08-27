package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.address.entity.UserAddress;
import com.cat.hard.address.service.AddressService;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.service.CartService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.order.calculator.OrderAmountCalculator;
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
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.model.OrderAmountResult;
import com.cat.hard.order.model.OrderIdempotencyLock;
import com.cat.hard.order.model.OrderItemAmount;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.stock.model.StockDeductionItem;
import com.cat.hard.stock.service.StockService;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.mapper.UserMapper;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final long PAYMENT_TIMEOUT_MINUTES = 5L;

	@Resource
	private CartService cartService;

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
	private AddressService addressService;

	@Resource
	private OrderAddressMapper orderAddressMapper;

	@Resource
	private UserMapper userMapper;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private StockService stockService;

	@Resource
	private OrderIdempotencyService orderIdempotencyService;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderCancellationTransactionService orderCancellationTransactionService;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private OrderBusinessLogService orderBusinessLogService;

	@Resource
	private TransactionTemplate transactionTemplate;

	public Order createOrder(OrderCreateRequest request) {
		OrderIdempotencyLock idempotencyLock = orderIdempotencyService.acquire(
				currentUser.getUserId(),
				request.getIdempotencyToken());

		try {
			List<CartItemResponse> selectedItems = getValidatedSelectedCartItems();
			OrderAmountResult amountResult = orderAmountCalculator.calculate(selectedItems);
			return transactionTemplate.execute(status -> createOrderInTransaction(
					request,
					selectedItems,
					amountResult));
		}
		catch (RuntimeException exception) {
			releaseIdempotencyAfterFailure(idempotencyLock);
			throw exception;
		}
	}

	private Order createOrderInTransaction(
			OrderCreateRequest request,
			List<CartItemResponse> selectedItems,
			OrderAmountResult amountResult) {

		Order order = createMainOrder(amountResult);
		createOrderItems(order.getId(), selectedItems, amountResult);
		createOrderAddress(order.getId(), request.getAddressId());

		List<StockDeductionItem> deductionItems = new ArrayList<>();
		for (CartItemResponse item : selectedItems) {
			deductionItems.add(new StockDeductionItem(
					item.getProductId(),
					item.getProductName(),
					item.getQuantity()));
		}
		stockService.decreaseForOrder(order.getOrderNo(), deductionItems);
		createOrderCreateLog(order);
		registerOrderCreatedLogAfterCommit(order);
		registerOrderTimeoutAfterCommit(order);
		clearPurchasedCartItemsAfterCommit(order.getOrderNo(), selectedItems);
		return order;
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
		return orderLockService.executeWithStatusLock(
				orderNo,
				() -> orderCancellationTransactionService.cancel(orderNo, userId));
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

	private void releaseIdempotencyAfterFailure(
			OrderIdempotencyLock idempotencyLock) {
		if (idempotencyLock == null) {
			return;
		}

		try {
			orderIdempotencyService.release(idempotencyLock);
		}
		catch (RuntimeException exception) {
			log.warn(
					"订单创建失败后释放幂等token失败，key={}",
					idempotencyLock.getKey(),
					exception);
		}
	}

	private void clearPurchasedCartItemsAfterCommit(
			String orderNo,
			List<CartItemResponse> selectedItems) {

		List<Long> productIds = new ArrayList<>();
		for (CartItemResponse item : selectedItems) {
			productIds.add(item.getProductId());
		}
		List<Long> purchasedProductIds = List.copyOf(productIds);

		transactionCallbackService.executeAfterCommit(
				() -> clearPurchasedCartItems(orderNo, purchasedProductIds));
	}

	private void registerOrderTimeoutAfterCommit(Order order) {
		String orderNo = order.getOrderNo();
		LocalDateTime expireAt = order.getExpireAt();
		transactionCallbackService.executeAfterCommit(
				() -> addOrderTimeout(orderNo, expireAt));
	}

	private void registerOrderCreatedLogAfterCommit(Order order) {
		String orderNo = order.getOrderNo();
		Long userId = order.getUserId();
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logCreated(orderNo, userId));
	}

	private void addOrderTimeout(String orderNo, LocalDateTime expireAt) {
		try {
			orderTimeoutRedisService.add(orderNo, expireAt);
		}
		catch (RuntimeException exception) {
			log.warn(
					"订单{}创建成功，但注册超时任务失败，expireAt={}",
					orderNo,
					expireAt,
					exception);
		}
	}

	private void clearPurchasedCartItems(
			String orderNo,
			List<Long> productIds) {
		try {
			cartService.deleteItems(productIds);
		}
		catch (RuntimeException exception) {
			log.warn("订单{}创建成功，但购物车清理失败", orderNo, exception);
		}
	}

	public List<CartItemResponse> getSelectedCartItems() {
		List<CartItemResponse> cartItems = cartService.listItems();
		List<CartItemResponse> selectedItems = new ArrayList<>();
		for (CartItemResponse cartItem : cartItems) {
			if (Boolean.TRUE.equals(cartItem.getSelected())) {
				selectedItems.add(cartItem);
			}
		}

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

	public OrderAmountResult calculateSelectedCartAmount() {
		List<CartItemResponse> selectedItems = getValidatedSelectedCartItems();
		return orderAmountCalculator.calculate(selectedItems);
	}

	public Order createMainOrder(OrderAmountResult amountResult) {
		Order order = new Order();
		order.setOrderNo(orderNumberGenerator.generate());
		order.setUserId(currentUser.getUserId());
		order.setTotalAmount(amountResult.getTotalAmount());
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES));
		orderMapper.insert(order);
		return order;
	}

	public List<OrderItem> createOrderItems(
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
			orderItems.add(orderItem);
		}

		orderItemMapper.insert(orderItems);
		return orderItems;
	}

	public OrderAddress createOrderAddress(Long orderId, Long addressId) {
		UserAddress sourceAddress = addressService.getOwnedAddress(addressId);

		OrderAddress orderAddress = new OrderAddress();
		orderAddress.setOrderId(orderId);
		orderAddress.setSourceAddressId(sourceAddress.getId());
		orderAddress.setReceiverName(sourceAddress.getReceiverName());
		orderAddress.setPhone(sourceAddress.getPhone());
		orderAddress.setProvince(sourceAddress.getProvince());
		orderAddress.setCity(sourceAddress.getCity());
		orderAddress.setDistrict(sourceAddress.getDistrict());
		orderAddress.setDetailAddress(sourceAddress.getDetailAddress());
		orderAddressMapper.insert(orderAddress);
		return orderAddress;
	}

	public OrderOperateLog createOrderCreateLog(Order order) {
		User user = userMapper.selectById(order.getUserId());
		if (user == null) {
			throw new BusinessException(
					ErrorCode.UNAUTHORIZED,
					"当前用户不存在");
		}

		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(order.getUserId());
		operateLog.setOperatorName(user.getNickname());
		operateLog.setOperation(OrderOperation.CREATE);
		operateLog.setFromStatus(null);
		operateLog.setToStatus(order.getStatus());
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
