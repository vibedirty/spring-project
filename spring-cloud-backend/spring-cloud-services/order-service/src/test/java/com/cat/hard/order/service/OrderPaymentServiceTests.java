package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.order.common.service.TransactionCallbackService;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.outbox.service.OutboxEventService;

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
class OrderPaymentServiceTests {

	@BeforeAll
	static void initializeTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				OrderMapper.class.getName());
		assistant.setCurrentNamespace(OrderMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Order.class);
	}

	@InjectMocks
	private OrderPaymentTransactionService paymentService;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderOperateLogMapper orderOperateLogMapper;

	@Mock
	private OrderItemMapper orderItemMapper;

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@Mock
	private OrderBusinessLogService orderBusinessLogService;

	@Mock
	private OutboxEventService outboxEventService;

	@Test
	void shouldPayOrderSuccessfullyAndSaveOutboxEvent() {
		String orderNo = "ORD202602280001";
		Long userId = 10001L;

		Order order = new Order();
		order.setId(1L);
		order.setOrderNo(orderNo);
		order.setUserId(userId);
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.now().plusMinutes(5));

		when(orderMapper.selectByOrderNoAndUserId(orderNo, userId)).thenReturn(order);
		when(orderMapper.update(any(), any())).thenReturn(1);

		OrderItem item = new OrderItem();
		item.setProductId(20001L);
		item.setQuantity(2);
		when(orderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

		UserSummary userSummary = new UserSummary(userId, "user1", "用户1", "USER", "ACTIVE");
		boolean result = paymentService.pay(orderNo, userId, userSummary);

		assertThat(result).isTrue();
		verify(outboxEventService).saveEvent(eq("OrderPaid"), eq("ORDER"), eq(orderNo), any());
	}
}
