package com.cat.hard.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.dto.OrderDetailResponse;
import com.cat.hard.order.dto.OrderItemSummaryResponse;
import com.cat.hard.order.dto.OrderListRequest;
import com.cat.hard.order.dto.OrderListResponse;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.service.OrderPaymentService;
import com.cat.hard.order.service.OrderReceiptService;
import com.cat.hard.order.service.OrderService;
import com.cat.hard.user.enums.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtSessionTokenService jwtTokenProvider;

	@MockitoBean
	private OrderService orderService;

	@MockitoBean
	private OrderPaymentService orderPaymentService;

	@MockitoBean
	private OrderReceiptService orderReceiptService;

	@Test
	void shouldPageMyOrdersWithStatusFilter() throws Exception {
		OrderItemSummaryResponse item = new OrderItemSummaryResponse(
				20001L,
				"测试商品",
				"https://example.com/product.png",
				new BigDecimal("19.90"),
				2,
				new BigDecimal("39.80"));
		OrderListResponse order = new OrderListResponse(
				"ORD2026082418000000000",
				OrderStatus.PENDING_PAYMENT,
				"待付款",
				new BigDecimal("39.80"),
				LocalDateTime.of(2026, 8, 24, 18, 0),
				LocalDateTime.of(2026, 8, 24, 18, 5),
				List.of(item));
		Page<OrderListResponse> page = new Page<OrderListResponse>(1, 10, 1);
		page.setRecords(List.of(order));
		when(orderService.pageMyOrders(any(OrderListRequest.class)))
				.thenReturn(page);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(get("/api/orders")
					.header("Authorization", "Bearer " + token)
					.param("page", "1")
					.param("size", "10")
					.param("status", "PENDING_PAYMENT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.result[0].orderNo")
						.value("ORD2026082418000000000"))
				.andExpect(jsonPath("$.data.result[0].status")
						.value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.result[0].statusDescription")
						.value("待付款"))
				.andExpect(jsonPath("$.data.result[0].totalAmount")
						.value("39.80"))
				.andExpect(jsonPath("$.data.result[0].items[0].productName")
						.value("测试商品"));

		verify(orderService).pageMyOrders(argThat(request ->
				request.getPage() == 1
						&& request.getSize() == 10
						&& request.getStatus() == OrderStatus.PENDING_PAYMENT));
	}

	@Test
	void shouldReturnOwnedOrderDetailWithoutInternalFields() throws Exception {
		Order order = new Order();
		order.setId(101L);
		order.setOrderNo("ORD2026082418000000000");
		order.setUserId(7L);
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setTotalAmount(new BigDecimal("39.80"));
		order.setCreatedAt(LocalDateTime.of(2026, 8, 24, 18, 0));

		OrderItem item = new OrderItem();
		item.setId(201L);
		item.setOrderId(101L);
		item.setProductId(20001L);
		item.setProductName("测试商品");
		item.setUnitPrice(new BigDecimal("19.90"));
		item.setQuantity(2);
		item.setSubtotalAmount(new BigDecimal("39.80"));

		OrderAddress address = new OrderAddress();
		address.setId(301L);
		address.setOrderId(101L);
		address.setSourceAddressId(30001L);
		address.setReceiverName("张三");
		address.setPhone("13800138000");
		address.setProvince("广东省");
		address.setCity("深圳市");
		address.setDistrict("南山区");
		address.setDetailAddress("科技园1号");

		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setId(401L);
		operateLog.setOrderId(101L);
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(7L);
		operateLog.setOperatorName("测试用户");
		operateLog.setOperation(OrderOperation.CREATE);
		operateLog.setToStatus(OrderStatus.PENDING_PAYMENT);
		operateLog.setReason("用户创建订单");
		operateLog.setCreatedAt(LocalDateTime.of(2026, 8, 24, 18, 0));

		OrderDetailResponse response = OrderDetailResponse.from(
				order,
				List.of(item),
				address,
				List.of(operateLog));
		when(orderService.getMyOrderDetail("ORD2026082418000000000"))
				.thenReturn(response);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(get("/api/orders/ORD2026082418000000000")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.orderNo")
						.value("ORD2026082418000000000"))
				.andExpect(jsonPath("$.data.statusDescription").value("待付款"))
				.andExpect(jsonPath("$.data.items[0].productName").value("测试商品"))
				.andExpect(jsonPath("$.data.address.receiverName").value("张三"))
				.andExpect(jsonPath("$.data.operateLogs[0].operation").value("CREATE"))
				.andExpect(jsonPath("$.data.id").doesNotExist())
				.andExpect(jsonPath("$.data.userId").doesNotExist())
				.andExpect(jsonPath("$.data.address.sourceAddressId").doesNotExist())
				.andExpect(jsonPath("$.data.operateLogs[0].operatorId").doesNotExist());

		verify(orderService).getMyOrderDetail("ORD2026082418000000000");
	}

	@Test
	void shouldCreateOrderAndReturnRequiredFields() throws Exception {
		Order order = new Order();
		order.setId(100L);
		order.setOrderNo("ORD2026082416000000000");
		order.setUserId(7L);
		order.setTotalAmount(new BigDecimal("99.50"));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.of(2026, 8, 24, 16, 5));
		when(orderService.createOrder(any(OrderCreateRequest.class)))
				.thenReturn(order);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "addressId": 30001
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data.orderNo")
						.value("ORD2026082416000000000"))
				.andExpect(jsonPath("$.data.totalAmount").value(99.5))
				.andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.expireAt")
						.value("2026-08-24T16:05:00"))
				.andExpect(jsonPath("$.data.id").doesNotExist())
				.andExpect(jsonPath("$.data.userId").doesNotExist());

		verify(orderService).createOrder(any(OrderCreateRequest.class));
	}

	@Test
	void shouldRejectCreateOrderWithoutAddressId() throws Exception {
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("收货地址ID不能为空"));

		verify(orderService, never()).createOrder(any(OrderCreateRequest.class));
	}

	@Test
	void shouldRequireLoginToCreateOrder() throws Exception {
		mockMvc.perform(post("/api/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "addressId": 30001
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("请先登录"));

		verify(orderService, never()).createOrder(any(OrderCreateRequest.class));
	}

	@Test
	void shouldAllowUserToConfirmReceipt() throws Exception {
		String orderNo = "ORD2026082418000000000";
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders/{orderNo}/confirm-receipt", orderNo)
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(orderReceiptService).confirmReceipt(orderNo);
	}

	@Test
	void shouldRequireLoginToConfirmReceipt() throws Exception {
		String orderNo = "ORD2026082418000000000";

		mockMvc.perform(post("/api/orders/{orderNo}/confirm-receipt", orderNo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("请先登录"));

		verify(orderReceiptService, never()).confirmReceipt(any());
	}

	@Test
	void shouldCancelOwnedPendingPaymentOrder() throws Exception {
		String orderNo = "ORD2026082418000000000";
		when(orderService.cancelOrder(orderNo)).thenReturn(true);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders/{orderNo}/cancel", orderNo)
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(orderService).cancelOrder(orderNo);
	}

	@Test
	void shouldRequireLoginToCancelOrder() throws Exception {
		String orderNo = "ORD2026082418000000000";

		mockMvc.perform(post("/api/orders/{orderNo}/cancel", orderNo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("请先登录"));

		verify(orderService, never()).cancelOrder(orderNo);
	}

	@Test
	void shouldPayOwnedValidOrder() throws Exception {
		String orderNo = "ORD2026082418000000000";
		when(orderPaymentService.pay(orderNo)).thenReturn(true);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders/{orderNo}/pay", orderNo)
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(orderPaymentService).pay(orderNo);
	}

	@Test
	void shouldRequireLoginToPayOrder() throws Exception {
		String orderNo = "ORD2026082418000000000";

		mockMvc.perform(post("/api/orders/{orderNo}/pay", orderNo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("请先登录"));

		verify(orderPaymentService, never()).pay(orderNo);
	}

	@Test
	void shouldReturnBusinessConflictWhenOrderCannotBePaid() throws Exception {
		String orderNo = "ORD2026082418000000000";
		when(orderPaymentService.pay(orderNo)).thenThrow(new BusinessException(
				ErrorCode.BUSINESS_CONFLICT,
				"当前订单状态不允许支付"));
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/orders/{orderNo}/pay", orderNo)
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.message")
						.value("当前订单状态不允许支付"));

		verify(orderPaymentService).pay(orderNo);
	}
}
