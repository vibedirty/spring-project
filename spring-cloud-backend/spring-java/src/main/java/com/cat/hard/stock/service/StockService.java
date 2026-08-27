package com.cat.hard.stock.service;

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.page.PageRequest;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.common.util.TextUtils;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.product.service.ProductCacheService;
import com.cat.hard.stock.dto.StockAdjustmentRequest;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.mapper.StockLogMapper;
import com.cat.hard.stock.model.StockDeductionItem;
import com.cat.hard.stock.model.StockRestorationItem;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StockLogMapper stockLogMapper;

	@Resource
	private ProductCacheService productCacheService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

    @Transactional
    public Product increase(Long productId, StockAdjustmentRequest request) {
        Integer changeQuantity = request.getChangeQuantity();
        if (changeQuantity == null || changeQuantity <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "库存增加数量必须大于0");
        }

        String reason = TextUtils.trimToNull(request.getReason());
        if (reason == null) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "库存调整原因不能为空");
        }

        LambdaUpdateWrapper<Product> updateWrapper =
                new LambdaUpdateWrapper<Product>(Product.class);
        updateWrapper.eq(Product::getId, productId)
                .le(Product::getStock, Integer.MAX_VALUE - changeQuantity)
                .setIncrBy(Product::getStock, changeQuantity)
                .set(Product::getUpdatedAt, LocalDateTime.now());

        int affectedRows = productMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "商品不存在");
            }
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "库存增加后超出允许范围");
        }

        Product updatedProduct = productMapper.selectById(productId);
        if (updatedProduct == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "商品不存在");
        }

        StockLog stockLog = new StockLog();
        stockLog.setProductId(productId);
        stockLog.setChangeQuantity(changeQuantity);
        stockLog.setBeforeStock(updatedProduct.getStock() - changeQuantity);
        stockLog.setAfterStock(updatedProduct.getStock());
        stockLog.setReason(reason);
        stockLogMapper.insert(stockLog);

		evictProductDetailAfterCommit(productId);
        return updatedProduct;
    }

    @Transactional
    public Product decrease(Long productId, StockAdjustmentRequest request) {
        Integer changeQuantity = request.getChangeQuantity();
        if (changeQuantity == null || changeQuantity >= 0) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "库存减少数量必须小于0");
        }

        String reason = TextUtils.trimToNull(request.getReason());
        if (reason == null) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "库存调整原因不能为空");
        }

        long requiredStock = -(long) changeQuantity;
        LambdaUpdateWrapper<Product> updateWrapper =
                new LambdaUpdateWrapper<Product>(Product.class);
        updateWrapper.eq(Product::getId, productId)
                .ge(Product::getStock, requiredStock)
                .setIncrBy(Product::getStock, changeQuantity)
                .set(Product::getUpdatedAt, LocalDateTime.now());

        int affectedRows = productMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "商品不存在");
            }
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品库存不足");
        }

        Product updatedProduct = productMapper.selectById(productId);
        if (updatedProduct == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "商品不存在");
        }

        StockLog stockLog = new StockLog();
        stockLog.setProductId(productId);
        stockLog.setChangeQuantity(changeQuantity);
        stockLog.setBeforeStock(updatedProduct.getStock() - changeQuantity);
        stockLog.setAfterStock(updatedProduct.getStock());
        stockLog.setReason(reason);
        stockLogMapper.insert(stockLog);

		evictProductDetailAfterCommit(productId);
        return updatedProduct;
    }

    @Transactional
    public void decreaseForOrder(
            String orderNo,
            List<StockDeductionItem> items) {
        String businessNo = TextUtils.trimToNull(orderNo);
        if (businessNo == null) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "订单号不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "待扣减库存商品不能为空");
        }

        List<StockLog> stockLogs = new ArrayList<>();
        for (StockDeductionItem item : items) {
            validateOrderDeductionItem(item);

            LambdaUpdateWrapper<Product> updateWrapper =
                    new LambdaUpdateWrapper<Product>(Product.class);
            updateWrapper.eq(Product::getId, item.getProductId())
                    .eq(Product::getStatus, ProductStatus.ON_SALE)
                    .ge(Product::getStock, item.getQuantity())
                    .setDecrBy(Product::getStock, item.getQuantity())
                    .set(Product::getUpdatedAt, LocalDateTime.now());

            int affectedRows = productMapper.update(null, updateWrapper);
            if (affectedRows == 0) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_CONFLICT,
                        productLabel(item) + "：库存不足或已不可售");
            }

            Product updatedProduct = productMapper.selectById(item.getProductId());
            if (updatedProduct == null) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_CONFLICT,
                        productLabel(item) + "：扣减库存后商品状态异常");
            }

            StockLog stockLog = new StockLog();
            stockLog.setProductId(item.getProductId());
            stockLog.setChangeQuantity(-item.getQuantity());
            stockLog.setBeforeStock(updatedProduct.getStock() + item.getQuantity());
            stockLog.setAfterStock(updatedProduct.getStock());
            stockLog.setReason("创建订单扣减库存");
            stockLog.setBusinessNo(businessNo);
            stockLogs.add(stockLog);
        }

        stockLogMapper.insert(stockLogs);
		evictProductDetailsAfterCommit(stockLogs);
    }

	@Transactional
	public void restoreForOrder(
			String orderNo,
			List<StockRestorationItem> items) {
		String businessNo = TextUtils.trimToNull(orderNo);
		if (businessNo == null) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"订单号不能为空");
		}
		if (items == null || items.isEmpty()) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"待恢复库存商品不能为空");
		}

		List<StockLog> stockLogs = new ArrayList<>();
		for (StockRestorationItem item : items) {
			validateOrderRestorationItem(item);

			LambdaUpdateWrapper<Product> updateWrapper =
					new LambdaUpdateWrapper<Product>(Product.class);
			updateWrapper.eq(Product::getId, item.getProductId())
					.le(Product::getStock, Integer.MAX_VALUE - item.getQuantity())
					.setIncrBy(Product::getStock, item.getQuantity())
					.set(Product::getUpdatedAt, LocalDateTime.now());

			int affectedRows = productMapper.update(null, updateWrapper);
			if (affectedRows == 0) {
				Product product = productMapper.selectById(item.getProductId());
				if (product == null) {
					throw new BusinessException(
							ErrorCode.RESOURCE_NOT_FOUND,
							productLabel(item) + "：商品不存在");
				}
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						productLabel(item) + "：恢复库存后超出允许范围");
			}

			Product updatedProduct = productMapper.selectById(item.getProductId());
			if (updatedProduct == null) {
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						productLabel(item) + "：恢复库存后商品状态异常");
			}

			StockLog stockLog = new StockLog();
			stockLog.setProductId(item.getProductId());
			stockLog.setChangeQuantity(item.getQuantity());
			stockLog.setBeforeStock(updatedProduct.getStock() - item.getQuantity());
			stockLog.setAfterStock(updatedProduct.getStock());
			stockLog.setReason("取消订单恢复库存");
			stockLog.setBusinessNo(businessNo);
			stockLogs.add(stockLog);
		}

		stockLogMapper.insert(stockLogs);
		evictProductDetailsAfterCommit(stockLogs);
	}

	private void evictProductDetailAfterCommit(Long productId) {
		transactionCallbackService.executeAfterCommit(
				() -> productCacheService.evictDetail(productId));
	}

	private void evictProductDetailsAfterCommit(List<StockLog> stockLogs) {
		Set<Long> productIds = new HashSet<Long>();
		for (StockLog stockLog : stockLogs) {
			productIds.add(stockLog.getProductId());
		}
		Set<Long> immutableProductIds = Set.copyOf(productIds);
		transactionCallbackService.executeAfterCommit(() -> {
			for (Long productId : immutableProductIds) {
				productCacheService.evictDetail(productId);
			}
		});
	}

    private void validateOrderDeductionItem(StockDeductionItem item) {
        if (item == null || item.getProductId() == null) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    "扣减库存商品ID不能为空");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAMETER_ERROR,
                    productLabel(item) + "：扣减库存数量必须大于0");
        }
    }

	private void validateOrderRestorationItem(StockRestorationItem item) {
		if (item == null || item.getProductId() == null) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"恢复库存商品ID不能为空");
		}
		if (item.getQuantity() == null || item.getQuantity() <= 0) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					productLabel(item) + "：恢复库存数量必须大于0");
		}
	}

    private String productLabel(StockDeductionItem item) {
        if (item.getProductName() == null || item.getProductName().isBlank()) {
            return "商品（ID：" + item.getProductId() + "）";
        }
        return "商品“" + item.getProductName()
                + "”（ID：" + item.getProductId() + "）";
    }

	private String productLabel(StockRestorationItem item) {
		if (item.getProductName() == null || item.getProductName().isBlank()) {
			return "商品（ID：" + item.getProductId() + "）";
		}
		return "商品“" + item.getProductName()
				+ "”（ID：" + item.getProductId() + "）";
	}

    public Page<StockLog> pageLog(PageRequest request) {
        LambdaQueryWrapper<StockLog> queryWrapper = new LambdaQueryWrapper<>(StockLog.class);
        queryWrapper.orderByDesc(StockLog::getCreatedAt)
                .orderByDesc(StockLog::getId);
       Page<StockLog> pages =  stockLogMapper.selectPage(request.toPage(), queryWrapper);
       fillProductInfo(pages.getRecords());
        return pages;
    }

    private void fillProductInfo(List<StockLog> list){
        if(list.isEmpty()) return;

        Set<Long> set = new HashSet<>();
        for(StockLog log : list){
            set.add(log.getProductId());
        }
        List<Product> products = productMapper.selectByIds(set);
        Map<Long, String> map = new HashMap<>();
        for(Product product : products){
            map.put(product.getId(), product.getName());
        }
        for(StockLog log : list){
            log.setProductName(map.get(log.getProductId()));
        }
    }
}
