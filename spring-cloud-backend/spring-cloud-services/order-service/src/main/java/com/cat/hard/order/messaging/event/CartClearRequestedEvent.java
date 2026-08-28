package com.cat.hard.order.messaging.event;

import java.time.LocalDateTime;
import java.util.List;

public record CartClearRequestedEvent(
		String eventId,
		String orderNo,
		Long userId,
		List<Long> productIds,
		LocalDateTime requestedAt,
		String traceId
) {
}
