package com.cat.hard.product.messaging.config;

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
	public static final String ROUTING_KEY_ORDER_PAID = "order.event.paid";
	public static final String QUEUE_PRODUCT_ORDER_PAID = "product.order-paid.queue";
	public static final String EXCHANGE_PRODUCT_ORDER_PAID_DLX = "product.order-paid.dlx.exchange";
	public static final String ROUTING_KEY_PRODUCT_ORDER_PAID_FAILED = "product.order-paid.failed";
	public static final String QUEUE_PRODUCT_ORDER_PAID_DLQ = "product.order-paid.dlq";

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
	public DirectExchange productOrderPaidDeadLetterExchange() {
		return new DirectExchange(EXCHANGE_PRODUCT_ORDER_PAID_DLX, true, false);
	}

	@Bean
	public Queue productOrderPaidDeadLetterQueue() {
		return new Queue(QUEUE_PRODUCT_ORDER_PAID_DLQ, true);
	}

	@Bean
	public Binding productOrderPaidDeadLetterBinding(
			Queue productOrderPaidDeadLetterQueue,
			DirectExchange productOrderPaidDeadLetterExchange) {
		return BindingBuilder.bind(productOrderPaidDeadLetterQueue)
				.to(productOrderPaidDeadLetterExchange)
				.with(ROUTING_KEY_PRODUCT_ORDER_PAID_FAILED);
	}
}
