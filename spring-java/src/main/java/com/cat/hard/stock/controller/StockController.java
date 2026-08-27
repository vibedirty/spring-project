package com.cat.hard.stock.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.common.page.PageRequest;
import com.cat.hard.common.page.PageResponse;
import com.cat.hard.stock.dto.StockLogResponse;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.service.StockService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stock")
public class StockController {

    @Resource
    StockService stockService;

    @GetMapping
    public ApiResponse<PageResponse<StockLogResponse>> pageStockLog(@Valid @ModelAttribute PageRequest request) {
        Page<StockLog> page = stockService.pageLog(request);
        List<StockLogResponse> list = new ArrayList<>();
        for (StockLog log : page.getRecords()) {
            list.add(StockLogResponse.from(log));
        }
        PageResponse<StockLogResponse> resp = new PageResponse<>(list, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());

        return ApiResponse.success(resp);
    }
}