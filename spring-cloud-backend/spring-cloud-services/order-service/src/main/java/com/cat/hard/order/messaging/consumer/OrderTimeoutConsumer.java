package com.cat.hard.order.messaging.consumer;

import java.io.IOException;

import com.cat.hard.order.messaging.config.RabbitMqConfiguration;
import com.cat.hard.order.messaging.event.OrderTimeoutScheduledEvent;
import com.cat.hard.order.service.OrderCancellationTransactionService;
import com.cat.hard.order.service.OrderLockService;
import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutConsumer {

	private static final Logger log = LoggerFactory.getLogger(OrderTimeoutConsumer.class);

	private final OrderCancellationTransactionService cancellationTransactionService;
	private final OrderLockService orderLockService;

	public OrderTimeoutConsumer(
			OrderCancellationTransactionService cancellationTransactionService,
			OrderLockService orderLockService) {
		this.cancellationTransactionService = cancellationTransactionService;
		this.orderLockService = orderLockService;
	}

	@RabbitListener(queues = RabbitMqConfiguration.QUEUE_ORDER_TIMEOUT_PROCESS, ackMode = "MANUAL")
	public void handleOrderTimeout(
			@Payload OrderTimeoutScheduledEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

		log.info("Received order timeout delay message: orderNo={}, eventId={}",
				event.orderNo(), event.eventId());
		try {
			orderLockService.executeWithStatusLock(
					event.orderNo(),
					() -> cancellationTransactionService.cancelExpired(event.orderNo()));
			channel.basicAck(deliveryTag, false);
			log.info("Successfully processed order timeout event: orderNo={}", event.orderNo());
		}
		catch (Exception e) {
			log.error("Error processing order timeout event: orderNo={}", event.orderNo(), e);
			// 超时关单有定时任务兜底，消费失败时记录日志并确认消息，避免阻塞死信队列
			channel.basicAck(deliveryTag, false);
		}
	}
}
