package com.cat.hard.product.messaging.consumer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

	public ProductSalesUpdateConsumer(
			ProductService productService,
			EventConsumptionLogMapper eventConsumptionLogMapper,
			TransactionTemplate transactionTemplate) {
		this.productService = productService;
		this.eventConsumptionLogMapper = eventConsumptionLogMapper;
		this.transactionTemplate = transactionTemplate;
	}

	@RabbitListener(queues = RabbitMqConfiguration.QUEUE_PRODUCT_ORDER_PAID, ackMode = "MANUAL")
	public void handleOrderPaid(
			@Payload OrderPaidEvent event,
			Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

		log.info("Received order paid event: orderNo={}, eventId={}", event.orderNo(), event.eventId());

		try {
			boolean processed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
				try {
					EventConsumptionLog logRecord = new EventConsumptionLog();
					logRecord.setEventId(event.eventId());
					logRecord.setConsumerName(CONSUMER_NAME);
					logRecord.setStatus("SUCCESS");
					logRecord.setCreatedAt(LocalDateTime.now());
					eventConsumptionLogMapper.insert(logRecord);
				}
				catch (DuplicateKeyException e) {
					log.info("Duplicate order paid event ignored: eventId={}, orderNo={}",
							event.eventId(), event.orderNo());
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
			log.error("Failed to process order paid event: orderNo={}, eventId={}", event.orderNo(), event.eventId(), e);
			channel.basicNack(deliveryTag, false, true);
		}
	}
}
