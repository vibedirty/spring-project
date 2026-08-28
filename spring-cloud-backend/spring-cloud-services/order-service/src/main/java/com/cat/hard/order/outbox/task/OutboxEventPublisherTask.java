package com.cat.hard.order.outbox.task;

import java.util.List;

import com.cat.hard.order.entity.OutboxEvent;
import com.cat.hard.order.messaging.config.RabbitMqConfiguration;
import com.cat.hard.order.messaging.event.CartClearRequestedEvent;
import com.cat.hard.order.messaging.event.OrderCancelledEvent;
import com.cat.hard.order.messaging.event.OrderCreatedEvent;
import com.cat.hard.order.messaging.event.OrderPaidEvent;
import com.cat.hard.order.messaging.event.OrderTimeoutScheduledEvent;
import com.cat.hard.order.messaging.publisher.OrderEventPublisher;
import com.cat.hard.order.outbox.service.OutboxEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisherTask {

	private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherTask.class);
	private static final int BATCH_SIZE = 50;

	private final OutboxEventService outboxEventService;
	private final OrderEventPublisher orderEventPublisher;
	private final ObjectMapper objectMapper;

	public OutboxEventPublisherTask(
			OutboxEventService outboxEventService,
			OrderEventPublisher orderEventPublisher) {
		this.outboxEventService = outboxEventService;
		this.orderEventPublisher = orderEventPublisher;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	@Scheduled(fixedDelay = 1000)
	public void publishPendingEvents() {
		List<OutboxEvent> pendingEvents = outboxEventService.findPendingEvents(BATCH_SIZE);
		if (pendingEvents.isEmpty()) {
			return;
		}

		for (OutboxEvent event : pendingEvents) {
			try {
				publishSingleEvent(event);
				outboxEventService.markPublished(event.getId());
			}
			catch (Exception e) {
				outboxEventService.handlePublishFailure(event.getId(), event.getRetryCount(), e);
			}
		}
	}

	private void publishSingleEvent(OutboxEvent event) throws Exception {
		String eventType = event.getEventType();
		String payloadJson = event.getPayload();

		switch (eventType) {
			case "OrderCreated": {
				OrderCreatedEvent message = objectMapper.readValue(payloadJson, OrderCreatedEvent.class);
				orderEventPublisher.publish(
						RabbitMqConfiguration.EXCHANGE_ORDER_EVENT,
						RabbitMqConfiguration.ROUTING_KEY_ORDER_CREATED,
						event.getEventId(),
						message);
				break;
			}
			case "OrderPaid": {
				OrderPaidEvent message = objectMapper.readValue(payloadJson, OrderPaidEvent.class);
				orderEventPublisher.publish(
						RabbitMqConfiguration.EXCHANGE_ORDER_EVENT,
						RabbitMqConfiguration.ROUTING_KEY_ORDER_PAID,
						event.getEventId(),
						message);
				break;
			}
			case "OrderCancelled": {
				OrderCancelledEvent message = objectMapper.readValue(payloadJson, OrderCancelledEvent.class);
				orderEventPublisher.publish(
						RabbitMqConfiguration.EXCHANGE_ORDER_EVENT,
						RabbitMqConfiguration.ROUTING_KEY_ORDER_CANCELLED,
						event.getEventId(),
						message);
				break;
			}
			case "CartClearRequested": {
				CartClearRequestedEvent message = objectMapper.readValue(payloadJson, CartClearRequestedEvent.class);
				orderEventPublisher.publish(
						RabbitMqConfiguration.EXCHANGE_ORDER_EVENT,
						RabbitMqConfiguration.ROUTING_KEY_CART_CLEAR,
						event.getEventId(),
						message);
				break;
			}
			case "OrderTimeoutScheduled": {
				OrderTimeoutScheduledEvent message = objectMapper.readValue(payloadJson, OrderTimeoutScheduledEvent.class);
				orderEventPublisher.publish(
						RabbitMqConfiguration.EXCHANGE_ORDER_TIMEOUT_DELAY,
						RabbitMqConfiguration.ROUTING_KEY_ORDER_TIMEOUT_SCHEDULE,
						event.getEventId(),
						message);
				break;
			}
			default:
				log.warn("Unknown outbox event type: {}", eventType);
		}
	}
}
