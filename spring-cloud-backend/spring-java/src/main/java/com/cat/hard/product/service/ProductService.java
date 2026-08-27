package com.cat.hard.product.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.DistributedLockService;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.common.util.TextUtils;
import com.cat.hard.product.dto.ProductCreateRequest;
import com.cat.hard.product.dto.ProductListRequest;
import com.cat.hard.product.dto.ProductPageRequest;
import com.cat.hard.product.dto.ProductUpdateRequest;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.enums.ProductSort;
import com.cat.hard.product.mapper.ProductMapper;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private static final String DETAIL_LOCK_KEY_PREFIX =
			"lock:cache:product:detail:v1:";
	private static final long DETAIL_LOCK_WAIT_SECONDS = 2L;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

	@Resource
	private ProductCacheService productCacheService;

	@Resource
	private DistributedLockService distributedLockService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

    @Transactional
    public Product create(ProductCreateRequest request) {
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品分类不存在");
        }

        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setCategoryName(category.getName());
        product.setName(request.getName().trim());
        product.setImageUrl(TextUtils.trimToNull(request.getImageUrl()));
        product.setDescription(TextUtils.trimToNull(request.getDescription()));
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSales(0);
        product.setStatus(ProductStatus.DRAFT);

        productMapper.insert(product);
        return product;
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品分类不存在");
        }

        LambdaUpdateWrapper<Product> updateWrapper =
                new LambdaUpdateWrapper<Product>(Product.class);
        updateWrapper.eq(Product::getId, id)
                .set(Product::getCategoryId, category.getId())
                .set(Product::getName, request.getName().trim())
                .set(Product::getImageUrl, TextUtils.trimToNull(request.getImageUrl()))
                .set(Product::getDescription, TextUtils.trimToNull(request.getDescription()))
                .set(Product::getPrice, request.getPrice())
                .set(Product::getUpdatedAt, LocalDateTime.now());

        int affectedRows = productMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        Product updatedProduct = productMapper.selectById(id);
        if (updatedProduct == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        updatedProduct.setCategoryName(category.getName());
		evictDetailAfterCommit(id);
        return updatedProduct;
    }

    @Transactional
	public Product changeStatus(Long id, ProductStatus targetStatus) {
        if (targetStatus == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "目标状态不能为空");
        }
        if (targetStatus == ProductStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品状态只能变更为上架或下架");
        }

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        Category category = categoryMapper.selectById(product.getCategoryId());
        if (product.getStatus() == targetStatus) {
            if (category != null) {
                product.setCategoryName(category.getName());
            }
            return product;
        }

        if (targetStatus == ProductStatus.ON_SALE) {
            validateProductForOnSale(product, category);
        } else if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "只有已上架商品可以下架");
        }

        LambdaUpdateWrapper<Product> updateWrapper =
                new LambdaUpdateWrapper<Product>(Product.class);
        updateWrapper.eq(Product::getId, id)
                .eq(Product::getStatus, product.getStatus())
                .set(Product::getStatus, targetStatus)
                .set(Product::getUpdatedAt, LocalDateTime.now());

        int affectedRows = productMapper.update(null, updateWrapper);
        if (affectedRows == 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品状态已发生变化，请刷新后重试");
        }

        Product updatedProduct = productMapper.selectById(id);
        if (updatedProduct == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        if (category != null) {
            updatedProduct.setCategoryName(category.getName());
        }
		evictDetailAfterCommit(id);
		return updatedProduct;
	}

	public Page<Product> page(ProductPageRequest request) {
		LambdaQueryWrapper<Product> queryWrapper =
				new LambdaQueryWrapper<Product>(Product.class);
		String name = TextUtils.trimToNull(request.getName());
		if (name != null) {
			queryWrapper.like(Product::getName, name);
		}
		if (request.getCategoryId() != null) {
			queryWrapper.eq(Product::getCategoryId, request.getCategoryId());
		}
		if (request.getStatus() != null) {
			queryWrapper.eq(Product::getStatus, request.getStatus());
		}
		queryWrapper.orderByDesc(Product::getId);

		Page<Product> productPage = productMapper.selectPage(
				request.toPage(),
				queryWrapper);
		fillCategoryNames(productPage.getRecords());
		return productPage;
	}

	public Page<Product> pageOnSale(ProductListRequest request) {
		LambdaQueryWrapper<Product> queryWrapper =
				new LambdaQueryWrapper<Product>(Product.class);
		queryWrapper.eq(Product::getStatus, ProductStatus.ON_SALE);

		if (request.getCategoryId() != null) {
			Category category = categoryMapper.selectById(request.getCategoryId());
			if (category == null || category.getStatus() != CategoryStatus.ENABLED) {
				return request.toPage();
			}
			queryWrapper.eq(Product::getCategoryId, category.getId());
		}
		String keyword = TextUtils.trimToNull(request.getKeyword());
		if (keyword != null) {
			queryWrapper.like(Product::getName, keyword);
		}

		if (request.getSort() == ProductSort.PRICE_ASC) {
			queryWrapper.orderByAsc(Product::getPrice)
					.orderByDesc(Product::getId);
		} else if (request.getSort() == ProductSort.PRICE_DESC) {
			queryWrapper.orderByDesc(Product::getPrice)
					.orderByDesc(Product::getId);
		} else {
			queryWrapper.orderByDesc(Product::getId);
		}

		Page<Product> productPage = productMapper.selectPage(
				request.toPage(),
				queryWrapper);
		fillCategoryNames(productPage.getRecords());
		return productPage;
	}

	public Product detail(Long id) {
		Product product = productMapper.selectById(id);
		if (product == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
		}

		fillCategoryNames(List.of(product));
		return product;
	}

	public Product detailOnSale(Long id) {
		Optional<Product> cachedProduct = productCacheService.getDetail(id);
		if (cachedProduct.isPresent()) {
			return cachedProduct.get();
		}
		return distributedLockService.executeWithLock(
				DETAIL_LOCK_KEY_PREFIX + id,
				DETAIL_LOCK_WAIT_SECONDS,
				() -> loadAndCacheOnSaleDetail(id),
				() -> loadAndCacheOnSaleDetail(id));
	}

	private Product loadAndCacheOnSaleDetail(Long id) {
		Optional<Product> cachedProduct = productCacheService.getDetail(id);
		if (cachedProduct.isPresent()) {
			return cachedProduct.get();
		}
		LambdaQueryWrapper<Product> queryWrapper =
				new LambdaQueryWrapper<Product>(Product.class);
		queryWrapper.eq(Product::getId, id)
				.eq(Product::getStatus, ProductStatus.ON_SALE);

		Product product = productMapper.selectOne(queryWrapper);
		if (product == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
		}

		fillCategoryNames(List.of(product));
		productCacheService.putDetail(product);
		return product;
	}

	@Transactional
	public void delete(Long id) {
		int affectedRows = productMapper.deleteById(id);
		if (affectedRows == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
		}
		evictDetailAfterCommit(id);
	}

	private void evictDetailAfterCommit(Long productId) {
		transactionCallbackService.executeAfterCommit(
				() -> productCacheService.evictDetail(productId));
	}

	private void fillCategoryNames(List<Product> products) {
		if (products.isEmpty()) {
			return;
		}

		Set<Long> categoryIds = new HashSet<Long>();
		for (Product product : products) {
			categoryIds.add(product.getCategoryId());
		}

		List<Category> categories = categoryMapper.selectByIds(categoryIds);
		Map<Long, String> categoryNames = new HashMap<Long, String>();
		for (Category category : categories) {
			categoryNames.put(category.getId(), category.getName());
		}
		for (Product product : products) {
			product.setCategoryName(categoryNames.get(product.getCategoryId()));
		}
	}

	private void validateProductForOnSale(Product product, Category category) {
        if (category == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品所属分类不存在，不能上架");
        }
        if (category.getStatus() != CategoryStatus.ENABLED) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品所属分类未启用，不能上架");
        }
        if (TextUtils.trimToNull(product.getName()) == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品名称无效，不能上架");
        }
        if (product.getPrice() == null || product.getPrice().signum() < 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品价格无效，不能上架");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "商品库存无效，不能上架");
        }
    }
}
