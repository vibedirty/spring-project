package com.cat.hard.order.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publish(String exchange, String routingKey, String eventId, Object message) {
		CorrelationData correlationData = new CorrelationData(eventId);
		try {
			rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
			log.info("Published message to RabbitMQ: exchange={}, routingKey={}, eventId={}",
					exchange, routingKey, eventId);
		}
		catch (Exception e) {
			log.error("Failed to publish message: exchange={}, routingKey={}, eventId={}",
					exchange, routingKey, eventId, e);
			throw new RuntimeException("投递消息到 RabbitMQ 失败", e);
		}
	}
}
