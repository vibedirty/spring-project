package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.order.auth.security.CurrentUser;
import com.cat.hard.order.common.service.TransactionCallbackService;
import com.cat.hard.order.dto.OrderShipmentRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.integration.account.service.AccountQueryService;
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTests {

	@BeforeAll
	static void initializeTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				OrderMapper.class.getName());
		assistant.setCurrentNamespace(OrderMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Order.class);
	}

	@InjectMocks
	private AdminOrderService adminOrderService;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderItemMapper orderItemMapper;

	@Mock
	private OrderAddressMapper orderAddressMapper;

	@Mock
	private OrderOperateLogMapper orderOperateLogMapper;

	@Mock
	private CurrentUser currentUser;

	@Mock
	private AccountQueryService accountQueryService;

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@Mock
	private OrderBusinessLogService orderBusinessLogService;

	@Test
	void shouldShipPendingShipmentOrder() {
		String orderNo = "ORD202602280001";
		Long adminId = 99999L;

		when(currentUser.getUserId()).thenReturn(adminId);
		when(accountQueryService.getUserSummary(adminId)).thenReturn(
				new UserSummary(adminId, "admin", "系统管理员", "ADMIN", "ACTIVE"));

		Order order = new Order();
		order.setId(1L);
		order.setOrderNo(orderNo);
		order.setStatus(OrderStatus.SHIPPED);
		order.setShippedAt(LocalDateTime.now());

		when(orderMapper.update(any(), any())).thenReturn(1);
		when(orderMapper.selectByOrderNo(orderNo)).thenReturn(order);

		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany("顺丰速运");
		request.setTrackingNumber("SF1000000000");

		Order shippedOrder = adminOrderService.ship(orderNo, request);

		assertThat(shippedOrder).isNotNull();
		assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		verify(orderOperateLogMapper).insert(any(OrderOperateLog.class));
	}
}
