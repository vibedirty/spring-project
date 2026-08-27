package com.cat.hard.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.common.page.PageResponse;
import com.cat.hard.order.dto.AdminOrderDetailResponse;
import com.cat.hard.order.dto.AdminOrderPageRequest;
import com.cat.hard.order.dto.OrderShipmentRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.service.AdminOrderService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

	@Resource
	private AdminOrderService adminOrderService;

	@GetMapping
	public ApiResponse<PageResponse<Order>> page(
			@Valid @ModelAttribute AdminOrderPageRequest request) {
		Page<Order> orderPage = adminOrderService.page(request);
		return ApiResponse.success(PageResponse.from(orderPage));
	}

	@GetMapping("/{orderNo}")
	public ApiResponse<AdminOrderDetailResponse> detail(
			@PathVariable String orderNo) {
		return ApiResponse.success(adminOrderService.getOrderDetail(orderNo));
	}

	@PostMapping("/{orderNo}/ship")
	public ApiResponse<Order> ship(
			@PathVariable String orderNo,
			@Valid @RequestBody OrderShipmentRequest request) {
		return ApiResponse.success(adminOrderService.ship(orderNo, request));
	}
}
