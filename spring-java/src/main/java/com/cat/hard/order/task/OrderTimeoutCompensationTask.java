package com.cat.hard.order.task;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.service.OrderTimeoutCancellationService;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "order.timeout.compensation",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class OrderTimeoutCompensationTask {

	private static final Logger log =
			LoggerFactory.getLogger(OrderTimeoutCompensationTask.class);
	private static final int DEFAULT_BATCH_SIZE = 100;

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderTimeoutCancellationService orderTimeoutCancellationService;

	@Value("${order.timeout.compensation.batch-size:100}")
	private int batchSize = DEFAULT_BATCH_SIZE;

	@Scheduled(
			fixedDelayString =
					"${order.timeout.compensation.fixed-delay-ms:60000}")
	public void compensateExpiredOrders() {
		Page<Order> page = new Page<>(1, batchSize, false);
		Page<Order> expiredOrders =
				orderMapper.selectExpiredPendingPaymentPage(
						page,
						LocalDateTime.now());
		for (Order order : expiredOrders.getRecords()) {
			try {
				orderTimeoutCancellationService.cancel(order.getOrderNo());
			}
			catch (RuntimeException exception) {
				log.error(
						"补偿取消超时订单{}失败",
						order.getOrderNo(),
						exception);
			}
		}
	}
}
