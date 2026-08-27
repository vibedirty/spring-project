package com.cat.hard.order.task;

import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.order.service.OrderTimeoutCancellationService;
import com.cat.hard.order.service.OrderTimeoutRedisService;

import jakarta.annotation.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		prefix = "order.timeout.scan",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class OrderTimeoutScanTask {

	private static final Logger log =
			LoggerFactory.getLogger(OrderTimeoutScanTask.class);
	private static final int DEFAULT_BATCH_SIZE = 100;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@Resource
	private OrderTimeoutCancellationService orderTimeoutCancellationService;

	@Value("${order.timeout.scan.batch-size:100}")
	private int batchSize = DEFAULT_BATCH_SIZE;

	@Scheduled(fixedDelayString = "${order.timeout.scan.fixed-delay-ms:5000}")
	public void scanExpiredOrders() {
		List<String> expiredOrderNos =
				orderTimeoutRedisService.findExpiredOrderNos(
						LocalDateTime.now(),
						batchSize);
		for (String orderNo : expiredOrderNos) {
			try {
				orderTimeoutCancellationService.cancel(orderNo);
			}
			catch (RuntimeException exception) {
				log.error("自动取消超时订单{}失败", orderNo, exception);
			}
		}
	}
}
