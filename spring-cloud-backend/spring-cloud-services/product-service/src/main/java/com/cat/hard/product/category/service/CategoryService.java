package com.cat.hard.product.category.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.product.category.dto.CategoryCreateRequest;
import com.cat.hard.product.category.dto.CategoryPageRequest;
import com.cat.hard.product.category.dto.CategoryUpdateRequest;
import com.cat.hard.product.category.entity.Category;
import com.cat.hard.product.category.enums.CategoryStatus;
import com.cat.hard.product.category.mapper.CategoryMapper;
import com.cat.hard.product.common.error.ErrorCode;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.common.service.TransactionCallbackService;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.mapper.ProductMapper;

import jakarta.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private CategoryCacheService categoryCacheService;

	@Resource
	private CategoryCacheLockService categoryCacheLockService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	public Optional<Category> findByName(String name) {
		QueryWrapper<Category> queryWrapper = new QueryWrapper<Category>();
		queryWrapper.eq("name", name);
		Category category = categoryMapper.selectOne(queryWrapper);
		return Optional.ofNullable(category);
	}

	public Page<Category> page(CategoryPageRequest request) {
		QueryWrapper<Category> queryWrapper = new QueryWrapper<Category>();

		String name = request.getName();
		if (name != null && !name.trim().isEmpty()) {
			queryWrapper.like("name", name.trim());
		}
		if (request.getStatus() != null) {
			queryWrapper.eq("status", request.getStatus());
		}
		queryWrapper.orderByAsc("sort");
		queryWrapper.orderByAsc("id");

		Page<Category> page = request.toPage();
		return categoryMapper.selectPage(page, queryWrapper);
	}

	public Integer getNextSort() {
		Integer maxSort = categoryMapper.selectMaxSort();
		if (maxSort == null) {
			return 1;
		}
		return maxSort + 1;
	}

	public List<Category> listEnabled() {
		Optional<List<Category>> cachedCategories =
				categoryCacheService.getEnabledList();
		if (cachedCategories.isPresent()) {
			return cachedCategories.get();
		}
		return categoryCacheLockService.executeWithEnabledListLock(
				this::loadAndCacheEnabledList);
	}

	private List<Category> loadAndCacheEnabledList() {
		Optional<List<Category>> cachedCategories =
				categoryCacheService.getEnabledList();
		if (cachedCategories.isPresent()) {
			return cachedCategories.get();
		}
		LambdaQueryWrapper<Category> queryWrapper =
				new LambdaQueryWrapper<Category>(Category.class);
		queryWrapper.eq(Category::getStatus, CategoryStatus.ENABLED);
		queryWrapper.orderByAsc(Category::getSort);
		queryWrapper.orderByAsc(Category::getId);
		List<Category> categories = categoryMapper.selectList(queryWrapper);
		categoryCacheService.putEnabledList(categories);
		return categories;
	}

	public List<Category> listDisabled() {
		QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("status", CategoryStatus.DISABLED);
		queryWrapper.orderByAsc("sort");
		queryWrapper.orderByAsc("id");
		return categoryMapper.selectList(queryWrapper);
	}

	public void validateCanDelete(Long id) {
		Category category = categoryMapper.selectById(id);
		if (category == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
		}

		LambdaQueryWrapper<Product> queryWrapper =
				new LambdaQueryWrapper<Product>(Product.class);
		queryWrapper.eq(Product::getCategoryId, id);
		Long productCount = productMapper.selectCount(queryWrapper);
		if (productCount > 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"分类下存在商品，不能删除");
		}
	}

	@Transactional
	public void delete(Long id) {
		validateCanDelete(id);
		int affectedRows = categoryMapper.deleteById(id);
		if (affectedRows == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
		}
		evictEnabledListAfterCommit();
	}

	@Transactional
	public Category create(CategoryCreateRequest request) {
		String name = request.getName().trim();
		Optional<Category> existingCategory = findByName(name);
		if (existingCategory.isPresent()) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "分类名称已存在");
		}

		Category category = new Category();
		category.setName(name);
		category.setSort(request.getSort());
		category.setStatus(request.getStatus());

		try {
			categoryMapper.insert(category);
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "分类名称已存在");
		}

		evictEnabledListAfterCommit();
		return category;
	}

	@Transactional
	public Category update(Long id, CategoryUpdateRequest request) {
		Category currentCategory = categoryMapper.selectById(id);
		if (currentCategory == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
		}

		String name = request.getName().trim();
		Optional<Category> existingCategory = findByName(name);
		if (existingCategory.isPresent()
				&& !existingCategory.get().getId().equals(id)) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "分类名称已存在");
		}

		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setSort(request.getSort());
		category.setStatus(request.getStatus());

		try {
			int affectedRows = categoryMapper.updateById(category);
			if (affectedRows == 0) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
			}
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "分类名称已存在");
		}

		evictEnabledListAfterCommit();
		return categoryMapper.selectById(id);
	}

	private void evictEnabledListAfterCommit() {
		transactionCallbackService.executeAfterCommit(
				categoryCacheService::evictEnabledList);
	}
}
