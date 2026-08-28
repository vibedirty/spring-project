package com.cat.hard.order.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.mapper.OrderMapper;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScanTask {

	private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScanTask.class);
	private static final int BATCH_SIZE = 100;

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderCancellationTransactionService cancellationTransactionService;

	@Scheduled(fixedDelay = 60000)
	public void scanAndCancelExpiredOrders() {
		LocalDateTime now = LocalDateTime.now();
		Page<Order> page = new Page<>(1, BATCH_SIZE);
		Page<Order> expiredPage = orderMapper.selectExpiredPendingPaymentPage(page, now);

		if (expiredPage.getRecords().isEmpty()) {
			return;
		}

		log.info("Scanned {} expired pending payment orders for compensation", expiredPage.getRecords().size());
		for (Order order : expiredPage.getRecords()) {
			try {
				orderLockService.executeWithStatusLock(
						order.getOrderNo(),
						() -> cancellationTransactionService.cancelExpired(order.getOrderNo()));
			}
			catch (Exception e) {
				log.error("Failed to cancel expired order {}: {}", order.getOrderNo(), e.getMessage());
			}
		}
	}
}
