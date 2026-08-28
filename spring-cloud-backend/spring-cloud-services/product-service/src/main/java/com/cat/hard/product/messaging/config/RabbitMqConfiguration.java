package com.cat.hard.product.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
}
