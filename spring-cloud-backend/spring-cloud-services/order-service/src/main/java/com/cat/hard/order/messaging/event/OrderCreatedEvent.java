package com.cat.hard.order.messaging.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
		String eventId,
		String orderNo,
		Long userId,
		BigDecimal totalAmount,
		LocalDateTime createdAt,
		String traceId
) {
}
