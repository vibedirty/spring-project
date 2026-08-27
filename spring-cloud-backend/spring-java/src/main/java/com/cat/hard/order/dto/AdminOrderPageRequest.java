package com.cat.hard.order.dto;

import java.time.LocalDateTime;

import com.cat.hard.common.page.PageRequest;
import com.cat.hard.order.enums.OrderStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

public class AdminOrderPageRequest extends PageRequest {

	@Size(max = 64, message = "订单号长度不能超过64个字符")
	@Pattern(regexp = ".*\\S.*", message = "订单号不能为空白")
	private String orderNo;

	@Positive(message = "用户ID必须大于0")
	private Long userId;

	private OrderStatus status;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startTime;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime endTime;

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	@AssertTrue(message = "开始时间不能晚于结束时间")
	public boolean isTimeRangeValid() {
		return startTime == null
				|| endTime == null
				|| !startTime.isAfter(endTime);
	}
}
