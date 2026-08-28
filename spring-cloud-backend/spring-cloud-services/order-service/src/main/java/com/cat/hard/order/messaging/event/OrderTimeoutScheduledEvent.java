package com.cat.hard.order.messaging.event;

import java.time.LocalDateTime;

public record OrderTimeoutScheduledEvent(
		String eventId,
		String orderNo,
		Long userId,
		LocalDateTime expireAt,
		String traceId
) {
}
