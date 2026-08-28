package com.cat.hard.product.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cat.hard.product.category.dto.CategoryCreateRequest;
import com.cat.hard.product.category.dto.CategoryUpdateRequest;
import com.cat.hard.product.category.entity.Category;
import com.cat.hard.product.category.enums.CategoryStatus;
import com.cat.hard.product.category.mapper.CategoryMapper;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.common.service.TransactionCallbackService;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.mapper.ProductMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void create_duplicateName_throwsBusinessException() {
		CategoryCreateRequest request = new CategoryCreateRequest();
		request.setName("电子数码");
		request.setSort(1);
		request.setStatus(CategoryStatus.ENABLED);

		Category existing = new Category();
		existing.setId(1L);
		existing.setName("电子数码");

		when(categoryMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

		assertThatThrownBy(() -> categoryService.create(request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("分类名称已存在");

		verify(categoryMapper, never()).insert(any(Category.class));
	}

	@Test
	void create_success_insertsAndRegistersCallback() {
		CategoryCreateRequest request = new CategoryCreateRequest();
		request.setName("电子数码");
		request.setSort(1);
		request.setStatus(CategoryStatus.ENABLED);

		when(categoryMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

		Category created = categoryService.create(request);

		assertThat(created.getName()).isEqualTo("电子数码");
		assertThat(created.getSort()).isEqualTo(1);
		verify(categoryMapper).insert(any(Category.class));
		verify(transactionCallbackService).executeAfterCommit(any());
	}

	@Test
	void delete_hasProducts_throwsBusinessException() {
		Category category = new Category();
		category.setId(10L);
		when(categoryMapper.selectById(10L)).thenReturn(category);
		when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

		assertThatThrownBy(() -> categoryService.delete(10L))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("分类下存在商品，不能删除");

		verify(categoryMapper, never()).deleteById(10L);
	}

	@Test
	void delete_noProducts_success() {
		Category category = new Category();
		category.setId(10L);
		when(categoryMapper.selectById(10L)).thenReturn(category);
		when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(categoryMapper.deleteById(10L)).thenReturn(1);

		categoryService.delete(10L);

		verify(categoryMapper).deleteById(10L);
		verify(transactionCallbackService).executeAfterCommit(any());
	}

	@Test
	void listEnabled_cacheHit_returnsCached() {
		Category c1 = new Category();
		c1.setId(1L);
		c1.setName("测试分类");
		when(categoryCacheService.getEnabledList()).thenReturn(Optional.of(List.of(c1)));

		List<Category> list = categoryService.listEnabled();
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getName()).isEqualTo("测试分类");
		verify(categoryMapper, never()).selectList(any(LambdaQueryWrapper.class));
	}
}
