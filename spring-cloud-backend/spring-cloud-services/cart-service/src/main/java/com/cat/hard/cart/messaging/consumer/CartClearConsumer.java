package com.cat.hard.cart.messaging.consumer;

import java.io.IOException;
import java.time.Duration;

import com.cat.hard.cart.messaging.config.RabbitMqConfiguration;
import com.cat.hard.cart.messaging.event.CartClearRequestedEvent;
import com.cat.hard.cart.service.CartService;
import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class CartClearConsumer {

	private static final Logger log = LoggerFactory.getLogger(CartClearConsumer.class);
	private static final String IDEMPOTENCY_PREFIX = "cart:event:consumed:";
	private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(1);

	private final CartService cartService;
	private final StringRedisTemplate stringRedisTemplate;

	public CartClearConsumer(
			CartService cartService,
			StringRedisTemplate stringRedisTemplate) {
		this.cartService = cartService;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@RabbitListener(queues = RabbitMqConfiguration.QUEUE_CART_ORDER_CLEAR, ackMode = "MANUAL")
	public void handleCartClear(
			@Payload CartClearRequestedEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

		log.info("Received cart clear message: orderNo={}, eventId={}, userId={}, productIds={}",
				event.orderNo(), event.eventId(), event.userId(), event.productIds());

		String key = IDEMPOTENCY_PREFIX + event.eventId();
		Boolean isNewEvent = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENCY_TTL);

		if (Boolean.FALSE.equals(isNewEvent)) {
			log.info("Duplicate cart clear event ignored: eventId={}, orderNo={}", event.eventId(), event.orderNo());
			channel.basicAck(deliveryTag, false);
			return;
		}

		try {
			cartService.deleteItems(event.userId(), event.productIds());
			channel.basicAck(deliveryTag, false);
			log.info("Successfully cleared cart items for order: orderNo={}", event.orderNo());
		}
		catch (Exception e) {
			log.error("Failed to clear cart items for order: orderNo={}", event.orderNo(), e);
			// 删除 Redis 幂等 key，允许重试
			stringRedisTemplate.delete(key);
			// 抛出异常由 RabbitMQ 重试或进行 nack
			channel.basicNack(deliveryTag, false, true);
		}
	}
}
