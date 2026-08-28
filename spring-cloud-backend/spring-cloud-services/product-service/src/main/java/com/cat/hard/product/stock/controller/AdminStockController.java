package com.cat.hard.product.stock.controller;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.common.api.PageResponse;
import com.cat.hard.product.common.page.PageRequest;
import com.cat.hard.product.stock.dto.StockLogResponse;
import com.cat.hard.product.stock.entity.StockLog;
import com.cat.hard.product.stock.service.StockService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stock")
public class AdminStockController {

	@Resource
	private StockService stockService;

	@GetMapping
	public ApiResponse<PageResponse<StockLogResponse>> pageStockLog(
			@Valid @ModelAttribute PageRequest request) {
		Page<StockLog> page = stockService.pageLog(request);
		List<StockLogResponse> list = new ArrayList<>();
		for (StockLog log : page.getRecords()) {
			list.add(StockLogResponse.from(log));
		}
		PageResponse<StockLogResponse> resp = new PageResponse<>(
				list,
				page.getCurrent(),
				page.getSize(),
				page.getTotal(),
				page.getPages());

		return ApiResponse.success(resp);
	}
}
