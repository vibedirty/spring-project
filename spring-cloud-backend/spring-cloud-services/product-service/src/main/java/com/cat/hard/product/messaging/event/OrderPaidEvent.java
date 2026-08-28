package com.cat.hard.product.messaging.event;

import java.time.LocalDateTime;
import java.util.List;

public record OrderPaidEvent(
		String eventId,
		String orderNo,
		Long userId,
		LocalDateTime paidAt,
		List<PaidItem> items,
		String traceId
) {

	public record PaidItem(
			Long productId,
			Integer quantity
	) {
	}
}
