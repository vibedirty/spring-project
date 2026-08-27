package com.cat.hard.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.cat.hard.order.dto.AdminOrderDetailResponse;
import com.cat.hard.order.dto.AdminOrderPageRequest;
import com.cat.hard.order.dto.OrderShipmentRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.service.AdminOrderService;
import com.cat.hard.user.enums.UserRole;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminOrderControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtSessionTokenService jwtTokenProvider;

	@MockitoBean
	private AdminOrderService adminOrderService;

	@Test
	void shouldAllowAdminToPageOrders() throws Exception {
		Page<Order> orderPage = new Page<Order>(1, 10, 1);
		orderPage.setRecords(List.of(order()));
		when(adminOrderService.page(any(AdminOrderPageRequest.class)))
				.thenReturn(orderPage);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/orders")
					.header("Authorization", "Bearer " + token)
					.param("orderNo", "ORD202608250001")
					.param("userId", "7")
					.param("status", "PENDING_PAYMENT")
					.param("startTime", "2026-08-01T00:00:00")
					.param("endTime", "2026-08-31T23:59:59")
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.result[0].orderNo")
						.value("ORD202608250001"))
				.andExpect(jsonPath("$.data.result[0].userId").value(7))
				.andExpect(jsonPath("$.data.result[0].status")
						.value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.pages").value(1));

		ArgumentCaptor<AdminOrderPageRequest> requestCaptor =
				ArgumentCaptor.forClass(AdminOrderPageRequest.class);
		verify(adminOrderService).page(requestCaptor.capture());
		AdminOrderPageRequest request = requestCaptor.getValue();
		assertThat(request.getOrderNo()).isEqualTo("ORD202608250001");
		assertThat(request.getUserId()).isEqualTo(7L);
		assertThat(request.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(request.getStartTime())
				.isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
		assertThat(request.getEndTime())
				.isEqualTo(LocalDateTime.of(2026, 8, 31, 23, 59, 59));
	}

	@Test
	void shouldRejectUserRole() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(get("/api/admin/orders")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(adminOrderService, never())
				.page(any(AdminOrderPageRequest.class));
	}

	@Test
	void shouldRejectInvalidQueryParameters() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/orders")
					.header("Authorization", "Bearer " + token)
					.param("page", "0")
					.param("userId", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(adminOrderService, never())
				.page(any(AdminOrderPageRequest.class));
	}

	@Test
	void shouldRejectReversedTimeRange() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/orders")
					.header("Authorization", "Bearer " + token)
					.param("startTime", "2026-08-02T00:00:00")
					.param("endTime", "2026-08-01T00:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message")
						.value("开始时间不能晚于结束时间"));

		verify(adminOrderService, never())
				.page(any(AdminOrderPageRequest.class));
	}

	@Test
	void shouldAllowAdminToGetOrderDetail() throws Exception {
		AdminOrderDetailResponse detailResponse = orderDetailResponse();
		when(adminOrderService.getOrderDetail("ORD202608250001"))
				.thenReturn(detailResponse);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/orders/ORD202608250001")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.orderNo").value("ORD202608250001"))
				.andExpect(jsonPath("$.data.userId").value(7))
				.andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
				.andExpect(jsonPath("$.data.totalAmount").value(59.80))
				.andExpect(jsonPath("$.data.items[0].productName").value("测试商品"))
				.andExpect(jsonPath("$.data.address.receiverName").value("张三"))
				.andExpect(jsonPath("$.data.operateLogs[0].operation").value("CREATE"));

		verify(adminOrderService).getOrderDetail("ORD202608250001");
	}

	@Test
	void shouldRejectUserRoleForOrderDetail() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(get("/api/admin/orders/ORD202608250001")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(adminOrderService, never())
				.getOrderDetail(any());
	}

	@Test
	void shouldAllowAdminToShipOrder() throws Exception {
		Order shippedOrder = order();
		shippedOrder.setStatus(OrderStatus.SHIPPED);
		shippedOrder.setShippingCompany("顺丰速运");
		shippedOrder.setTrackingNumber("SF1234567890");
		shippedOrder.setShippedAt(LocalDateTime.of(2026, 8, 25, 18, 0));
		when(adminOrderService.ship(
				any(String.class),
				any(OrderShipmentRequest.class)))
				.thenReturn(shippedOrder);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/orders/ORD202608250001/ship")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "shippingCompany": "顺丰速运",
							  "trackingNumber": "SF1234567890"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.orderNo").value("ORD202608250001"))
				.andExpect(jsonPath("$.data.status").value("SHIPPED"))
				.andExpect(jsonPath("$.data.shippingCompany").value("顺丰速运"))
				.andExpect(jsonPath("$.data.trackingNumber").value("SF1234567890"));

		ArgumentCaptor<OrderShipmentRequest> requestCaptor =
				ArgumentCaptor.forClass(OrderShipmentRequest.class);
		verify(adminOrderService).ship(
				org.mockito.ArgumentMatchers.eq("ORD202608250001"),
				requestCaptor.capture());
		assertThat(requestCaptor.getValue().getShippingCompany()).isEqualTo("顺丰速运");
		assertThat(requestCaptor.getValue().getTrackingNumber()).isEqualTo("SF1234567890");
	}

	@Test
	void shouldRejectUserRoleForShipping() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(post("/api/admin/orders/ORD202608250001/ship")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "shippingCompany": "顺丰速运",
							  "trackingNumber": "SF1234567890"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403));

		verify(adminOrderService, never()).ship(any(), any());
	}

	@Test
	void shouldRejectInvalidShipmentRequest() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/orders/ORD202608250001/ship")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "shippingCompany": " ",
							  "trackingNumber": ""
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(adminOrderService, never()).ship(any(), any());
	}

	private Order order() {
		Order order = new Order();
		order.setId(101L);
		order.setOrderNo("ORD202608250001");
		order.setUserId(7L);
		order.setTotalAmount(new BigDecimal("59.80"));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.of(2026, 8, 25, 17, 20));
		order.setCreatedAt(LocalDateTime.of(2026, 8, 25, 17, 0));
		order.setUpdatedAt(LocalDateTime.of(2026, 8, 25, 17, 0));
		return order;
	}

	private AdminOrderDetailResponse orderDetailResponse() {
		Order order = order();
		com.cat.hard.order.entity.OrderItem item = new com.cat.hard.order.entity.OrderItem();
		item.setOrderId(101L);
		item.setProductId(201L);
		item.setProductName("测试商品");
		item.setUnitPrice(new BigDecimal("29.90"));
		item.setQuantity(2);
		item.setSubtotalAmount(new BigDecimal("59.80"));

		com.cat.hard.order.entity.OrderAddress address = new com.cat.hard.order.entity.OrderAddress();
		address.setOrderId(101L);
		address.setSourceAddressId(10L);
		address.setReceiverName("张三");
		address.setPhone("13800138000");
		address.setProvince("广东省");
		address.setCity("深圳市");
		address.setDistrict("南山区");
		address.setDetailAddress("科技园中区1号");

		com.cat.hard.order.entity.OrderOperateLog log = new com.cat.hard.order.entity.OrderOperateLog();
		log.setOrderId(101L);
		log.setOperatorType(com.cat.hard.order.enums.OrderOperatorType.USER);
		log.setOperatorId(7L);
		log.setOperatorName("用户7");
		log.setOperation(com.cat.hard.order.enums.OrderOperation.CREATE);
		log.setToStatus(OrderStatus.PENDING_PAYMENT);
		log.setReason("创建订单");
		log.setCreatedAt(LocalDateTime.of(2026, 8, 25, 17, 0));

		return AdminOrderDetailResponse.from(
				order,
				List.of(item),
				address,
				List.of(log));
	}
}
