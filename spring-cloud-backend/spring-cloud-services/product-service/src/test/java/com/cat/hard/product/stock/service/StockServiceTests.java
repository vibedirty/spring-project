package com.cat.hard.product.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.common.service.TransactionCallbackService;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.mapper.ProductMapper;
import com.cat.hard.product.product.service.ProductCacheService;
import com.cat.hard.product.stock.dto.StockDeductionItem;
import com.cat.hard.product.stock.dto.StockOperationResultResponse;
import com.cat.hard.product.stock.dto.StockRestorationItem;
import com.cat.hard.product.stock.entity.StockLog;
import com.cat.hard.product.stock.entity.StockOperationLog;
import com.cat.hard.product.stock.enums.StockOperationStatus;
import com.cat.hard.product.stock.enums.StockOperationType;
import com.cat.hard.product.stock.mapper.StockLogMapper;
import com.cat.hard.product.stock.mapper.StockOperationLogMapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class StockServiceTests {

	@BeforeAll
	static void initializeProductTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				ProductMapper.class.getName());
		assistant.setCurrentNamespace(ProductMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Product.class);
	}

	@Mock
	private ProductMapper productMapper;

	@Mock
	private StockLogMapper stockLogMapper;

	@Mock
	private StockOperationLogMapper stockOperationLogMapper;

	@Mock
	private ProductCacheService productCacheService;

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@InjectMocks
	private StockService stockService;

	@Test
	void decreaseForOrder_idempotent_whenLogAlreadyExists_returnsDirectly() {
		String orderNo = "ORD-20260828-001";
		List<StockDeductionItem> items = List.of(
				new StockDeductionItem(1L, "商品A", 2));

		StockOperationLog successLog = new StockOperationLog();
		successLog.setOrderNo(orderNo);
		successLog.setOperationType(StockOperationType.DEDUCT);
		successLog.setStatus(StockOperationStatus.SUCCESS);
		successLog.setDetail("1:2");

		when(stockOperationLogMapper.insert(any(StockOperationLog.class)))
				.thenThrow(new DuplicateKeyException("duplicate"));
		when(stockOperationLogMapper.selectOne(any(LambdaQueryWrapper.class)))
				.thenReturn(successLog);

		stockService.decreaseForOrder(orderNo, items);

		verify(productMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
		verify(stockLogMapper, never()).insert(any(List.class));
	}

	@Test
	void decreaseForOrder_payloadConflict_throwsBusinessConflictException() {
		String orderNo = "ORD-20260828-001";
		List<StockDeductionItem> items = List.of(
				new StockDeductionItem(1L, "商品A", 5));

		StockOperationLog successLog = new StockOperationLog();
		successLog.setOrderNo(orderNo);
		successLog.setOperationType(StockOperationType.DEDUCT);
		successLog.setStatus(StockOperationStatus.SUCCESS);
		successLog.setDetail("1:2");

		when(stockOperationLogMapper.insert(any(StockOperationLog.class)))
				.thenThrow(new DuplicateKeyException("duplicate"));
		when(stockOperationLogMapper.selectOne(any(LambdaQueryWrapper.class)))
				.thenReturn(successLog);

		assertThatThrownBy(() -> stockService.decreaseForOrder(orderNo, items))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("幂等冲突：该订单已存在不同内容的扣减请求");

		verify(productMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void decreaseForOrder_success_updatesStockAndInsertsLog() {
		String orderNo = "ORD-20260828-002";
		List<StockDeductionItem> items = List.of(
				new StockDeductionItem(1L, "商品A", 2));

		when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		Product product = new Product();
		product.setId(1L);
		product.setStock(8);
		when(productMapper.selectById(1L)).thenReturn(product);

		stockService.decreaseForOrder(orderNo, items);

		verify(productMapper).update(any(), any(LambdaUpdateWrapper.class));
		verify(stockLogMapper).insert(any(List.class));
		verify(stockOperationLogMapper).insert(any(StockOperationLog.class));
		verify(stockOperationLogMapper).updateById(any(StockOperationLog.class));
		verify(transactionCallbackService).executeAfterCommit(any());
	}

	@Test
	void decreaseForOrder_outOfStock_throwsBusinessException() {
		String orderNo = "ORD-20260828-003";
		List<StockDeductionItem> items = List.of(
				new StockDeductionItem(1L, "商品A", 100));

		when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> stockService.decreaseForOrder(orderNo, items))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("库存不足或已不可售");

		verify(stockLogMapper, never()).insert(any(List.class));
	}

	@Test
	void restoreForOrder_withoutDeductSuccess_throwsBusinessConflictException() {
		String orderNo = "ORD-20260828-004";
		List<StockRestorationItem> items = List.of(
				new StockRestorationItem(1L, "商品A", 2));

		when(stockOperationLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		when(stockLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

		assertThatThrownBy(() -> stockService.restoreForOrder(orderNo, items))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("订单未成功扣减库存，无法执行库存恢复");

		verify(productMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void restoreForOrder_success_updatesStockAndInsertsLog() {
		String orderNo = "ORD-20260828-005";
		List<StockRestorationItem> items = List.of(
				new StockRestorationItem(1L, "商品A", 2));

		StockOperationLog deductLog = new StockOperationLog();
		deductLog.setOrderNo(orderNo);
		deductLog.setOperationType(StockOperationType.DEDUCT);
		deductLog.setStatus(StockOperationStatus.SUCCESS);
		deductLog.setDetail("1:2");

		when(stockOperationLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(deductLog);
		when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		Product product = new Product();
		product.setId(1L);
		product.setStock(10);
		when(productMapper.selectById(1L)).thenReturn(product);

		stockService.restoreForOrder(orderNo, items);

		verify(productMapper).update(any(), any(LambdaUpdateWrapper.class));
		verify(stockLogMapper).insert(any(List.class));
		verify(transactionCallbackService).executeAfterCommit(any());
	}

	@Test
	void queryStockResult_returnsStatusFromStockLogs() {
		String orderNo = "ORD-20260828-006";

		StockLog deductLog = new StockLog();
		deductLog.setId(1L);
		deductLog.setProductId(10L);
		deductLog.setChangeQuantity(-2);
		deductLog.setBusinessNo(orderNo);

		StockLog restoreLog = new StockLog();
		restoreLog.setId(2L);
		restoreLog.setProductId(10L);
		restoreLog.setChangeQuantity(2);
		restoreLog.setBusinessNo(orderNo);

		Product product = new Product();
		product.setId(10L);
		product.setName("测试商品");

		when(stockLogMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(deductLog, restoreLog));
		when(productMapper.selectByIds(any())).thenReturn(List.of(product));

		StockOperationResultResponse response = stockService.queryStockResult(orderNo);

		assertThat(response.getOrderNo()).isEqualTo(orderNo);
		assertThat(response.isDeducted()).isTrue();
		assertThat(response.isRestored()).isTrue();
		assertThat(response.getLogs()).hasSize(2);
	}

	@Test
	void decreaseForOrder_whenProcessing_throwsBusinessConflictException() {
		String orderNo = "ORD-20260828-007";
		List<StockDeductionItem> items = List.of(
				new StockDeductionItem(1L, "商品A", 1));

		StockOperationLog processingLog = new StockOperationLog();
		processingLog.setOrderNo(orderNo);
		processingLog.setOperationType(StockOperationType.DEDUCT);
		processingLog.setStatus(StockOperationStatus.PROCESSING);
		processingLog.setDetail("1:1");

		when(stockOperationLogMapper.insert(any(StockOperationLog.class)))
				.thenThrow(new DuplicateKeyException("duplicate"));
		when(stockOperationLogMapper.selectOne(any(LambdaQueryWrapper.class)))
				.thenReturn(processingLog);

		assertThatThrownBy(() -> stockService.decreaseForOrder(orderNo, items))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("正在处理中，请勿重复提交");

		verify(productMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}
}
