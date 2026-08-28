package com.cat.hard.order.messaging.event;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
		String eventId,
		String orderNo,
		Long userId,
		String reason,
		boolean automatic,
		LocalDateTime cancelledAt,
		String traceId
) {
}
