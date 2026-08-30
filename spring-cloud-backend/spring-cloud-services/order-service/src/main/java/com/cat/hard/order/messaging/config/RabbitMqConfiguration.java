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

	/**
	 * 创建 JSON 消息转换器，负责 RabbitMQ 消息与 Java 对象之间的序列化和反序列化。
	 */
	@Bean
	public MessageConverter jsonMessageConverter() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	/**
	 * 声明订单业务事件交换机，用于路由订单创建、支付、取消和购物车清理事件。
	 */
	@Bean
	public TopicExchange orderEventExchange() {
		return new TopicExchange(EXCHANGE_ORDER_EVENT, true, false);
	}

	/**
	 * 声明购物车清理队列，用于接收订单创建后异步清理购物车的消息。
	 */
	@Bean
	public Queue cartOrderClearQueue() {
		return new Queue(QUEUE_CART_ORDER_CLEAR, true);
	}

	/**
	 * 将购物车清理队列绑定到订单事件交换机，并使用购物车清理路由键接收消息。
	 */
	@Bean
	public Binding cartOrderClearBinding(Queue cartOrderClearQueue, TopicExchange orderEventExchange) {
		return BindingBuilder.bind(cartOrderClearQueue)
				.to(orderEventExchange)
				.with(ROUTING_KEY_CART_CLEAR);
	}

	/**
	 * 声明商品销量更新队列，用于接收订单支付完成事件并异步增加商品销量。
	 */
	@Bean
	public Queue productOrderPaidQueue() {
		return new Queue(QUEUE_PRODUCT_ORDER_PAID, true);
	}

	/**
	 * 将商品销量更新队列绑定到订单事件交换机，并使用订单支付路由键接收消息。
	 */
	@Bean
	public Binding productOrderPaidBinding(Queue productOrderPaidQueue, TopicExchange orderEventExchange) {
		return BindingBuilder.bind(productOrderPaidQueue)
				.to(orderEventExchange)
				.with(ROUTING_KEY_ORDER_PAID);
	}

	/**
	 * 声明订单超时延时交换机，超时调度消息首先发送到该交换机。
	 */
	@Bean
	public DirectExchange orderTimeoutDelayExchange() {
		return new DirectExchange(EXCHANGE_ORDER_TIMEOUT_DELAY, true, false);
	}

	/**
	 * 声明订单超时延时队列，消息等待 5 分钟后通过死信交换机转入超时处理队列。
	 */
	@Bean
	public Queue orderTimeoutDelayQueue() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("x-message-ttl", ORDER_TIMEOUT_TTL_MS);
		arguments.put("x-dead-letter-exchange", EXCHANGE_ORDER_TIMEOUT_DLX);
		arguments.put("x-dead-letter-routing-key", ROUTING_KEY_ORDER_TIMEOUT_PROCESS);
		return new Queue(QUEUE_ORDER_TIMEOUT_DELAY, true, false, false, arguments);
	}

	/**
	 * 将订单超时延时队列绑定到延时交换机，并使用超时调度路由键接收消息。
	 */
	@Bean
	public Binding orderTimeoutDelayBinding(Queue orderTimeoutDelayQueue, DirectExchange orderTimeoutDelayExchange) {
		return BindingBuilder.bind(orderTimeoutDelayQueue)
				.to(orderTimeoutDelayExchange)
				.with(ROUTING_KEY_ORDER_TIMEOUT_SCHEDULE);
	}

	/**
	 * 声明订单超时死信交换机，用于接收延时队列中 TTL 到期的消息。
	 */
	@Bean
	public DirectExchange orderTimeoutDlxExchange() {
		return new DirectExchange(EXCHANGE_ORDER_TIMEOUT_DLX, true, false);
	}

	/**
	 * 声明订单超时处理队列，由订单超时消费者监听并执行自动关单。
	 */
	@Bean
	public Queue orderTimeoutProcessQueue() {
		return new Queue(QUEUE_ORDER_TIMEOUT_PROCESS, true);
	}

	/**
	 * 将订单超时处理队列绑定到死信交换机，并使用超时处理路由键接收过期消息。
	 */
	@Bean
	public Binding orderTimeoutProcessBinding(Queue orderTimeoutProcessQueue, DirectExchange orderTimeoutDlxExchange) {
		return BindingBuilder.bind(orderTimeoutProcessQueue)
				.to(orderTimeoutDlxExchange)
				.with(ROUTING_KEY_ORDER_TIMEOUT_PROCESS);
	}
}
