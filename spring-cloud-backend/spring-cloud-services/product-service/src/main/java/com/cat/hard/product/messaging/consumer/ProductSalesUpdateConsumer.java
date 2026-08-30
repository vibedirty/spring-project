package com.cat.hard.product.messaging.consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.cat.hard.product.common.entity.EventConsumptionLog;
import com.cat.hard.product.common.mapper.EventConsumptionLogMapper;
import com.cat.hard.product.messaging.config.RabbitMqConfiguration;
import com.cat.hard.product.messaging.event.OrderPaidEvent;
import com.cat.hard.product.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.product.product.service.ProductService;
import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProductSalesUpdateConsumer {

	private static final Logger log = LoggerFactory.getLogger(ProductSalesUpdateConsumer.class);
	private static final String CONSUMER_NAME = "ProductSalesUpdateConsumer";

	private final ProductService productService;
	private final EventConsumptionLogMapper eventConsumptionLogMapper;
	private final TransactionTemplate transactionTemplate;
	private final RabbitTemplate rabbitTemplate;

	public ProductSalesUpdateConsumer(
			ProductService productService,
			EventConsumptionLogMapper eventConsumptionLogMapper,
			TransactionTemplate transactionTemplate,
			RabbitTemplate rabbitTemplate) {
		this.productService = productService;
		this.eventConsumptionLogMapper = eventConsumptionLogMapper;
		this.transactionTemplate = transactionTemplate;
		this.rabbitTemplate = rabbitTemplate;
	}

	@RabbitListener(queues = RabbitMqConfiguration.QUEUE_PRODUCT_ORDER_PAID, ackMode = "MANUAL")
	public void handleOrderPaid(
			@Payload OrderPaidEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
			@Header(name = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered) throws IOException {

		String eventId = resolveEventId(event);
		log.info("Received order paid event: orderNo={}, eventId={}", event.orderNo(), eventId);

		try {
			if (event.orderNo() == null || event.orderNo().isBlank()) {
				throw new IllegalArgumentException("OrderPaid event has no orderNo");
			}
			boolean processed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
				try {
					EventConsumptionLog logRecord = new EventConsumptionLog();
					logRecord.setEventId(eventId);
					logRecord.setConsumerName(CONSUMER_NAME);
					logRecord.setEventType("OrderPaid");
					logRecord.setStatus("SUCCESS");
					logRecord.setCreatedAt(LocalDateTime.now());
					eventConsumptionLogMapper.insert(logRecord);
				}
				catch (DuplicateKeyException e) {
					log.info("Duplicate order paid event ignored: eventId={}, orderNo={}",
							eventId, event.orderNo());
					return false;
				}

				if (event.items() != null && !event.items().isEmpty()) {
					List<ProductSalesUpdateRequest.SalesItem> salesItems = new ArrayList<>();
					for (OrderPaidEvent.PaidItem item : event.items()) {
						salesItems.add(new ProductSalesUpdateRequest.SalesItem(item.productId(), item.quantity()));
					}
					ProductSalesUpdateRequest request = new ProductSalesUpdateRequest(event.orderNo(), salesItems);
					productService.increaseSales(request);
				}
				return true;
			}));

			channel.basicAck(deliveryTag, false);
			if (processed) {
				log.info("Successfully updated product sales for order: orderNo={}", event.orderNo());
			}
		}
		catch (Exception e) {
			log.error("Failed to process order paid event: orderNo={}, eventId={}", event.orderNo(), eventId, e);
			if (!Boolean.TRUE.equals(redelivered)) {
				channel.basicNack(deliveryTag, false, true);
				return;
			}

			try {
				publishToDeadLetterQueue(event, eventId, e);
				channel.basicAck(deliveryTag, false);
				log.error("Order paid event moved to DLQ after retry: orderNo={}, eventId={}, dlq={}",
						event.orderNo(), eventId, RabbitMqConfiguration.QUEUE_PRODUCT_ORDER_PAID_DLQ);
			}
			catch (Exception publishException) {
				log.error("Failed to move order paid event to DLQ; message will be requeued: eventId={}",
						eventId, publishException);
				channel.basicNack(deliveryTag, false, true);
			}
		}
	}

	private String resolveEventId(OrderPaidEvent event) {
		if (event.eventId() != null && !event.eventId().isBlank()) {
			return event.eventId();
		}
		String legacyIdentity = "OrderPaid|" + String.valueOf(event.orderNo());
		String generatedEventId = UUID.nameUUIDFromBytes(legacyIdentity.getBytes(StandardCharsets.UTF_8)).toString();
		log.warn("Order paid event has no eventId; generated deterministic legacy id: orderNo={}, eventId={}",
				event.orderNo(), generatedEventId);
		return generatedEventId;
	}

	private void publishToDeadLetterQueue(OrderPaidEvent event, String eventId, Exception cause) throws Exception {
		CorrelationData correlationData = new CorrelationData("dlq-" + eventId);
		rabbitTemplate.convertAndSend(
				RabbitMqConfiguration.EXCHANGE_PRODUCT_ORDER_PAID_DLX,
				RabbitMqConfiguration.ROUTING_KEY_PRODUCT_ORDER_PAID_FAILED,
				event,
				message -> {
					message.getMessageProperties().setHeader("x-original-event-id", eventId);
					message.getMessageProperties().setHeader("x-failure-reason", cause.getClass().getSimpleName());
					return message;
				},
				correlationData);

		CorrelationData.Confirm confirm = correlationData.getFuture().get(10, TimeUnit.SECONDS);
		if (!confirm.ack()) {
			throw new IllegalStateException("RabbitMQ DLQ Publisher Confirm NACK: " + confirm.reason());
		}
		if (correlationData.getReturned() != null) {
			throw new IllegalStateException(
					"RabbitMQ DLQ message was returned: " + correlationData.getReturned().getReplyText());
		}
	}
}
