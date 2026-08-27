package com.cat.hard.category.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.hard.category.dto.CategoryCreateRequest;
import com.cat.hard.category.dto.CategoryUpdateRequest;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.mapper.ProductMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

	@Mock
	private CategoryMapper categoryMapper;

	@Mock
	private ProductMapper productMapper;

	@Mock
	private CategoryCacheService categoryCacheService;

	@Mock
	private CategoryCacheLockService categoryCacheLockService;

	@Spy
	private TransactionCallbackService transactionCallbackService =
			new TransactionCallbackService();

	@InjectMocks
	private CategoryService categoryService;

	@BeforeEach
	void executeCacheLoadOperationWhenLockIsRequested() {
		lenient().when(categoryCacheLockService.executeWithEnabledListLock(any()))
				.thenAnswer(invocation -> {
					Supplier<List<Category>> operation = invocation.getArgument(0);
					return operation.get();
				});
	}

	@Test
	void shouldReturnEnabledCategoriesFromCacheWithoutQueryingDatabase() {
		List<Category> cachedCategories = List.of(category());
		when(categoryCacheService.getEnabledList())
				.thenReturn(Optional.of(cachedCategories));

		List<Category> result = categoryService.listEnabled();

		assertEquals(cachedCategories, result);
		verify(categoryMapper, never()).selectList(any(LambdaQueryWrapper.class));
		verify(categoryCacheService, never()).putEnabledList(any());
		verify(categoryCacheLockService, never())
				.executeWithEnabledListLock(any());
	}

	@Test
	void shouldQueryAndCacheEnabledCategoriesOnCacheMiss() {
		List<Category> databaseCategories = List.of(category());
		when(categoryCacheService.getEnabledList()).thenReturn(Optional.empty());
		when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(databaseCategories);

		List<Category> result = categoryService.listEnabled();

		assertEquals(databaseCategories, result);
		verify(categoryMapper).selectList(any(LambdaQueryWrapper.class));
		verify(categoryCacheService).putEnabledList(databaseCategories);
	}

	@Test
	void shouldDoubleCheckCacheAfterAcquiringRefreshLock() {
		List<Category> refreshedCategories = List.of(category());
		when(categoryCacheService.getEnabledList())
				.thenReturn(Optional.empty(), Optional.of(refreshedCategories));

		List<Category> result = categoryService.listEnabled();

		assertEquals(refreshedCategories, result);
		verify(categoryCacheLockService).executeWithEnabledListLock(any());
		verify(categoryMapper, never()).selectList(any(LambdaQueryWrapper.class));
		verify(categoryCacheService, never()).putEnabledList(any());
	}

	@Test
	void shouldCacheEmptyEnabledCategoryList() {
		when(categoryCacheService.getEnabledList()).thenReturn(Optional.empty());
		when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of());

		assertEquals(List.of(), categoryService.listEnabled());

		verify(categoryCacheService).putEnabledList(List.of());
	}

	@Test
	void shouldAllowCategoryWithoutProductReferences() {
		when(categoryMapper.selectById(1L)).thenReturn(category());
		when(productMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(0L);

		assertDoesNotThrow(() -> categoryService.validateCanDelete(1L));

		verify(productMapper).selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any());
	}

	@Test
	void shouldRejectCategoryWithProductReferences() {
		when(categoryMapper.selectById(1L)).thenReturn(category());
		when(productMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(1L);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> categoryService.validateCanDelete(1L));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("分类下存在商品，不能删除", exception.getMessage());
	}

	@Test
	void shouldRejectMissingCategoryBeforeCheckingReferences() {
		when(categoryMapper.selectById(1L)).thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> categoryService.validateCanDelete(1L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("分类不存在", exception.getMessage());
		verify(productMapper, never()).selectCount(any());
	}

	@Test
	void shouldLogicallyDeleteCategoryWithoutProductReferences() {
		when(categoryMapper.selectById(1L)).thenReturn(category());
		when(productMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(0L);
		when(categoryMapper.deleteById(1L)).thenReturn(1);

		assertDoesNotThrow(() -> categoryService.delete(1L));

		verify(categoryMapper).deleteById(1L);
		verify(categoryCacheService).evictEnabledList();
	}

	@Test
	void shouldNotDeleteCategoryWithProductReferences() {
		when(categoryMapper.selectById(1L)).thenReturn(category());
		when(productMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(1L);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> categoryService.delete(1L));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		verify(categoryMapper, never()).deleteById(anyLong());
		verify(categoryCacheService, never()).evictEnabledList();
	}

	@Test
	void shouldRejectDeleteWhenCategoryDisappearsBeforeUpdate() {
		when(categoryMapper.selectById(1L)).thenReturn(category());
		when(productMapper.selectCount(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(0L);
		when(categoryMapper.deleteById(1L)).thenReturn(0);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> categoryService.delete(1L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("分类不存在", exception.getMessage());
		verify(categoryCacheService, never()).evictEnabledList();
	}

	@Test
	void shouldEvictEnabledListAfterCreateTransactionCommits() {
		CategoryCreateRequest request = createRequest();
		when(categoryMapper.insert(any(Category.class))).thenReturn(1);

		TransactionSynchronizationManager.initSynchronization();
		try {
			categoryService.create(request);
			verify(categoryCacheService, never()).evictEnabledList();

			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}
			verify(categoryCacheService).evictEnabledList();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldEvictEnabledListAfterSuccessfulUpdate() {
		Category currentCategory = category();
		Category updatedCategory = category();
		updatedCategory.setName("更新分类");
		when(categoryMapper.selectById(1L))
				.thenReturn(currentCategory, updatedCategory);
		when(categoryMapper.updateById(any(Category.class))).thenReturn(1);

		Category result = categoryService.update(1L, updateRequest());

		assertEquals("更新分类", result.getName());
		verify(categoryCacheService).evictEnabledList();
	}

	private CategoryCreateRequest createRequest() {
		CategoryCreateRequest request = new CategoryCreateRequest();
		request.setName("新分类");
		request.setSort(10);
		request.setStatus(CategoryStatus.ENABLED);
		return request;
	}

	private CategoryUpdateRequest updateRequest() {
		CategoryUpdateRequest request = new CategoryUpdateRequest();
		request.setName("更新分类");
		request.setSort(20);
		request.setStatus(CategoryStatus.ENABLED);
		return request;
	}

	private Category category() {
		Category category = new Category();
		category.setId(1L);
		category.setName("测试分类");
		return category;
	}
}
