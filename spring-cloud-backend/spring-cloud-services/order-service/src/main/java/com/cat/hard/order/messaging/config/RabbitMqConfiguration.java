package com.cat.hard.order.messaging.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {

	public static final String EXCHANGE_ORDER_EVENT = "order.event.exchange";
	public static final String ROUTING_KEY_ORDER_CREATED = "order.event.created";
	public static final String ROUTING_KEY_ORDER_PAID = "order.event.paid";
	public static final String ROUTING_KEY_ORDER_CANCELLED = "order.event.cancelled";
	public static final String ROUTING_KEY_CART_CLEAR = "cart.event.clear";

	public static final String EXCHANGE_ORDER_TIMEOUT_DELAY = "order.timeout.delay.exchange";
	public static final String ROUTING_KEY_ORDER_TIMEOUT_SCHEDULE = "order.timeout.schedule";
	public static final String QUEUE_ORDER_TIMEOUT_DELAY = "order.timeout.delay.queue";

	public static final String EXCHANGE_ORDER_TIMEOUT_DLX = "order.timeout.dlx.exchange";
	public static final String ROUTING_KEY_ORDER_TIMEOUT_PROCESS = "order.timeout.process";
	public static final String QUEUE_ORDER_TIMEOUT_PROCESS = "order.timeout.process.queue";

	public static final String QUEUE_CART_ORDER_CLEAR = "cart.order-clear.queue";
	public static final String QUEUE_PRODUCT_ORDER_PAID = "product.order-paid.queue";

	// 5 分钟延时（毫秒）
	public static final int ORDER_TIMEOUT_TTL_MS = 300_000;

	@Bean
	public MessageConverter jsonMessageConverter() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	@Bean
	public TopicExchange orderEventExchange() {
		return new TopicExchange(EXCHANGE_ORDER_EVENT, true, false);
	}

	@Bean
	public Queue cartOrderClearQueue() {
		return new Queue(QUEUE_CART_ORDER_CLEAR, true);
	}

	@Bean
	public Binding cartOrderClearBinding(Queue cartOrderClearQueue, TopicExchange orderEventExchange) {
		return BindingBuilder.bind(cartOrderClearQueue)
				.to(orderEventExchange)
				.with(ROUTING_KEY_CART_CLEAR);
	}

	@Bean
	public Queue productOrderPaidQueue() {
		return new Queue(QUEUE_PRODUCT_ORDER_PAID, true);
	}

	@Bean
	public Binding productOrderPaidBinding(Queue productOrderPaidQueue, TopicExchange orderEventExchange) {
		return BindingBuilder.bind(productOrderPaidQueue)
				.to(orderEventExchange)
				.with(ROUTING_KEY_ORDER_PAID);
	}

	@Bean
	public DirectExchange orderTimeoutDelayExchange() {
		return new DirectExchange(EXCHANGE_ORDER_TIMEOUT_DELAY, true, false);
	}

	@Bean
	public Queue orderTimeoutDelayQueue() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("x-message-ttl", ORDER_TIMEOUT_TTL_MS);
		arguments.put("x-dead-letter-exchange", EXCHANGE_ORDER_TIMEOUT_DLX);
		arguments.put("x-dead-letter-routing-key", ROUTING_KEY_ORDER_TIMEOUT_PROCESS);
		return new Queue(QUEUE_ORDER_TIMEOUT_DELAY, true, false, false, arguments);
	}

	@Bean
	public Binding orderTimeoutDelayBinding(Queue orderTimeoutDelayQueue, DirectExchange orderTimeoutDelayExchange) {
		return BindingBuilder.bind(orderTimeoutDelayQueue)
				.to(orderTimeoutDelayExchange)
				.with(ROUTING_KEY_ORDER_TIMEOUT_SCHEDULE);
	}

	@Bean
	public DirectExchange orderTimeoutDlxExchange() {
		return new DirectExchange(EXCHANGE_ORDER_TIMEOUT_DLX, true, false);
	}

	@Bean
	public Queue orderTimeoutProcessQueue() {
		return new Queue(QUEUE_ORDER_TIMEOUT_PROCESS, true);
	}

	@Bean
	public Binding orderTimeoutProcessBinding(Queue orderTimeoutProcessQueue, DirectExchange orderTimeoutDlxExchange) {
		return BindingBuilder.bind(orderTimeoutProcessQueue)
				.to(orderTimeoutDlxExchange)
				.with(ROUTING_KEY_ORDER_TIMEOUT_PROCESS);
	}
}
