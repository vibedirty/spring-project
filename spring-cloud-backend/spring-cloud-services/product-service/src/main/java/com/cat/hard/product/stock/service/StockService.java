package com.cat.hard.product.stock.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.product.common.error.ErrorCode;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.common.page.PageRequest;
import com.cat.hard.product.common.service.TransactionCallbackService;
import com.cat.hard.product.common.util.TextUtils;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.enums.ProductStatus;
import com.cat.hard.product.product.mapper.ProductMapper;
import com.cat.hard.product.product.service.ProductCacheService;
import com.cat.hard.product.stock.dto.StockAdjustmentRequest;
import com.cat.hard.product.stock.dto.StockDeductionItem;
import com.cat.hard.product.stock.dto.StockLogResponse;
import com.cat.hard.product.stock.dto.StockOperationResultResponse;
import com.cat.hard.product.stock.dto.StockRestorationItem;
import com.cat.hard.product.stock.entity.StockLog;
import com.cat.hard.product.stock.entity.StockOperationLog;
import com.cat.hard.product.stock.enums.StockOperationStatus;
import com.cat.hard.product.stock.enums.StockOperationType;
import com.cat.hard.product.stock.mapper.StockLogMapper;
import com.cat.hard.product.stock.mapper.StockOperationLogMapper;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StockService {
	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	@Resource
	private ProductMapper productMapper;

	@Resource
	private StockLogMapper stockLogMapper;

	@Resource
	private StockOperationLogMapper stockOperationLogMapper;

	@Resource
	private ProductCacheService productCacheService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private StockOperationStateService stockOperationStateService;

	@Resource
	private TransactionTemplate transactionTemplate;

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

		for (StockDeductionItem item : items) {
			validateOrderDeductionItem(item);
		}

		String payloadDigest = buildDeductionDigest(items);
		StockOperationStateService.ClaimResult claim = stockOperationStateService.claim(
				businessNo,
				StockOperationType.DEDUCT,
				payloadDigest);
		if (claim.alreadySucceeded()) {
			return;
		}

		try {
			transactionTemplate.executeWithoutResult(status ->
					performOrderDeduction(
							businessNo,
							items,
							claim.operationId(),
							claim.ownerToken()));
		}
		catch (RuntimeException exception) {
			markOperationFailed(claim.operationId(), claim.ownerToken(), exception);
			throw exception;
		}
	}

	private void performOrderDeduction(
			String businessNo,
			List<StockDeductionItem> items,
			Long operationId,
			String ownerToken) {

		List<StockLog> stockLogs = new ArrayList<>();
		for (StockDeductionItem item : items) {
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

		markOperationSucceeded(operationId, ownerToken);

		evictProductDetailsAfterCommit(stockLogs);
	}

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

		for (StockRestorationItem item : items) {
			validateOrderRestorationItem(item);
		}

		String payloadDigest = buildRestorationDigest(items);

		// 恢复前置条件校验：订单必须已成功扣减库存
		LambdaQueryWrapper<StockOperationLog> deductQuery = new LambdaQueryWrapper<StockOperationLog>()
				.eq(StockOperationLog::getOrderNo, businessNo)
				.eq(StockOperationLog::getOperationType, StockOperationType.DEDUCT);
		StockOperationLog deductOp = stockOperationLogMapper.selectOne(deductQuery);
		if (deductOp == null || !StockOperationStatus.SUCCESS.equals(deductOp.getStatus())) {
			LambdaQueryWrapper<StockLog> legacyDeductQuery = new LambdaQueryWrapper<StockLog>()
					.eq(StockLog::getBusinessNo, businessNo)
					.lt(StockLog::getChangeQuantity, 0);
			Long count = stockLogMapper.selectCount(legacyDeductQuery);
			if (count == null || count == 0) {
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						"订单未成功扣减库存，无法执行库存恢复");
			}
		}
		else if (deductOp.getDetail() != null && !deductOp.getDetail().equals(payloadDigest)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"恢复库存商品与原订单扣减商品明细不一致");
		}

		StockOperationStateService.ClaimResult claim = stockOperationStateService.claim(
				businessNo,
				StockOperationType.RESTORE,
				payloadDigest);
		if (claim.alreadySucceeded()) {
			return;
		}

		try {
			transactionTemplate.executeWithoutResult(status ->
					performOrderRestoration(
							businessNo,
							items,
							claim.operationId(),
							claim.ownerToken()));
		}
		catch (RuntimeException exception) {
			markOperationFailed(claim.operationId(), claim.ownerToken(), exception);
			throw exception;
		}
	}

	private void performOrderRestoration(
			String businessNo,
			List<StockRestorationItem> items,
			Long operationId,
			String ownerToken) {

		List<StockLog> stockLogs = new ArrayList<>();
		for (StockRestorationItem item : items) {
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

		markOperationSucceeded(operationId, ownerToken);

		evictProductDetailsAfterCommit(stockLogs);
	}

	private void markOperationSucceeded(Long operationId, String ownerToken) {
		LambdaUpdateWrapper<StockOperationLog> update = new LambdaUpdateWrapper<>();
		update.eq(StockOperationLog::getId, operationId)
				.eq(StockOperationLog::getStatus, StockOperationStatus.PROCESSING)
				.eq(StockOperationLog::getOwnerToken, ownerToken)
				.set(StockOperationLog::getStatus, StockOperationStatus.SUCCESS);
		if (stockOperationLogMapper.update(null, update) != 1) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库存操作状态已变化，无法提交结果");
		}
	}

	private void markOperationFailed(
			Long operationId,
			String ownerToken,
			RuntimeException originalException) {
		try {
			stockOperationStateService.markFailed(operationId, ownerToken);
		}
		catch (RuntimeException stateException) {
			log.error("记录库存操作失败状态异常，operationId={}", operationId, stateException);
			originalException.addSuppressed(stateException);
		}
	}

	private String buildDeductionDigest(List<StockDeductionItem> items) {
		return items.stream()
				.sorted(Comparator.comparing(StockDeductionItem::getProductId))
				.map(i -> i.getProductId() + ":" + i.getQuantity())
				.collect(Collectors.joining(","));
	}

	private String buildRestorationDigest(List<StockRestorationItem> items) {
		return items.stream()
				.sorted(Comparator.comparing(StockRestorationItem::getProductId))
				.map(i -> i.getProductId() + ":" + i.getQuantity())
				.collect(Collectors.joining(","));
	}

	public StockOperationResultResponse queryStockResult(String orderNo) {
		String businessNo = TextUtils.trimToNull(orderNo);
		if (businessNo == null) {
			throw new BusinessException(ErrorCode.PARAMETER_ERROR, "订单号不能为空");
		}

		LambdaQueryWrapper<StockOperationLog> opQuery = new LambdaQueryWrapper<StockOperationLog>()
				.eq(StockOperationLog::getOrderNo, businessNo)
				.orderByDesc(StockOperationLog::getOperationType);
		List<StockOperationLog> opLogs = stockOperationLogMapper.selectList(opQuery);
		StockOperationLog selected = opLogs.stream()
				.filter(op -> StockOperationType.RESTORE.equals(op.getOperationType()))
				.findFirst()
				.orElseGet(() -> opLogs.stream()
						.filter(op -> StockOperationType.DEDUCT.equals(op.getOperationType()))
						.findFirst()
						.orElse(null));
		if (selected == null) {
			return new StockOperationResultResponse(businessNo, null, "NOT_FOUND", null);
		}
		return new StockOperationResultResponse(
				businessNo,
				selected.getOperationType().name(),
				selected.getStatus().name(),
				selected.getDetail());
	}

	public Page<StockLog> pageLog(PageRequest request) {
		LambdaQueryWrapper<StockLog> queryWrapper = new LambdaQueryWrapper<>(StockLog.class);
		queryWrapper.orderByDesc(StockLog::getCreatedAt)
				.orderByDesc(StockLog::getId);
		Page<StockLog> pages = stockLogMapper.selectPage(request.toPage(), queryWrapper);
		fillProductInfo(pages.getRecords());
		return pages;
	}

	private void evictProductDetailAfterCommit(Long productId) {
		transactionCallbackService.executeAfterCommit(
				() -> productCacheService.evictDetail(productId));
	}

	private void evictProductDetailsAfterCommit(List<StockLog> stockLogs) {
		Set<Long> productIds = new HashSet<>();
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

	private void fillProductInfo(List<StockLog> list) {
		if (list.isEmpty()) {
			return;
		}

		Set<Long> set = new HashSet<>();
		for (StockLog log : list) {
			set.add(log.getProductId());
		}
		List<Product> products = productMapper.selectByIds(set);
		Map<Long, String> map = new HashMap<>();
		for (Product product : products) {
			map.put(product.getId(), product.getName());
		}
		for (StockLog log : list) {
			log.setProductName(map.get(log.getProductId()));
		}
	}
}
