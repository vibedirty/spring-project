package com.cat.hard.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.common.page.PageResponse;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.dto.OrderCreateResponse;
import com.cat.hard.order.dto.OrderDetailResponse;
import com.cat.hard.order.dto.OrderListRequest;
import com.cat.hard.order.dto.OrderListResponse;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.service.OrderPaymentService;
import com.cat.hard.order.service.OrderReceiptService;
import com.cat.hard.order.service.OrderService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Resource
	private OrderService orderService;

	@Resource
	private OrderPaymentService orderPaymentService;

	@Resource
	private OrderReceiptService orderReceiptService;

	@GetMapping
	public ApiResponse<PageResponse<OrderListResponse>> page(
			@Valid @ModelAttribute OrderListRequest request) {
		Page<OrderListResponse> orderPage = orderService.pageMyOrders(request);
		return ApiResponse.success(PageResponse.from(orderPage));
	}

	@PostMapping
	public ApiResponse<OrderCreateResponse> create(
			@Valid @RequestBody OrderCreateRequest request) {
		Order order = orderService.createOrder(request);
		return ApiResponse.success(OrderCreateResponse.from(order));
	}

	@GetMapping("/{orderNo}")
	public ApiResponse<OrderDetailResponse> detail(
			@PathVariable String orderNo) {
		return ApiResponse.success(orderService.getMyOrderDetail(orderNo));
	}

	@PostMapping("/{orderNo}/cancel")
	public ApiResponse<Void> cancel(@PathVariable String orderNo) {
		orderService.cancelOrder(orderNo);
		return ApiResponse.success();
	}

	@PostMapping("/{orderNo}/pay")
	public ApiResponse<Void> pay(@PathVariable String orderNo) {
		orderPaymentService.pay(orderNo);
		return ApiResponse.success();
	}

	@PostMapping("/{orderNo}/confirm-receipt")
	public ApiResponse<Void> confirmReceipt(@PathVariable String orderNo) {
		orderReceiptService.confirmReceipt(orderNo);
		return ApiResponse.success();
	}
}
