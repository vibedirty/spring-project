package com.cat.hard.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.dto.ProductListRequest;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.enums.ProductSort;
import com.cat.hard.product.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void shouldAllowAnonymousCategoryFilter() throws Exception {
		Page<Product> productPage = new Page<Product>(1, 10, 1);
		productPage.setRecords(List.of(onSaleProduct()));
		when(productService.pageOnSale(any(ProductListRequest.class)))
				.thenReturn(productPage);

		mockMvc.perform(get("/api/products")
					.param("categoryId", "1")
					.param("keyword", "测试")
					.param("sort", "PRICE_ASC")
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.result[0].categoryId").value(1))
				.andExpect(jsonPath("$.data.result[0].categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.result[0].status").value("ON_SALE"))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.total").value(1));

		verify(productService).pageOnSale(argThat(request ->
				Long.valueOf(1L).equals(request.getCategoryId())
						&& "测试".equals(request.getKeyword())
						&& request.getSort() == ProductSort.PRICE_ASC));
	}

	@Test
	void shouldRejectInvalidCategoryId() throws Exception {
		mockMvc.perform(get("/api/products")
					.param("categoryId", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("商品分类ID必须大于0"));

		verify(productService, never()).pageOnSale(any(ProductListRequest.class));
	}

	@Test
	void shouldRejectKeywordLongerThan128Characters() throws Exception {
		mockMvc.perform(get("/api/products")
					.param("keyword", "a".repeat(129)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message")
						.value("商品搜索关键词长度不能超过128个字符"));

		verify(productService, never()).pageOnSale(any(ProductListRequest.class));
	}

	@Test
	void shouldRejectSortOutsideWhitelist() throws Exception {
		mockMvc.perform(get("/api/products")
					.param("sort", "stock desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(productService, never()).pageOnSale(any(ProductListRequest.class));
	}

	@Test
	void shouldAllowAnonymousToGetOnSaleProductDetail() throws Exception {
		when(productService.detailOnSale(10L)).thenReturn(onSaleProduct());

		mockMvc.perform(get("/api/products/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.categoryId").value(1))
				.andExpect(jsonPath("$.data.categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.name").value("测试商品"))
				.andExpect(jsonPath("$.data.price").value(99.99))
				.andExpect(jsonPath("$.data.stock").value(8))
				.andExpect(jsonPath("$.data.status").value("ON_SALE"));

		verify(productService).detailOnSale(10L);
	}

	@Test
	void shouldReturnNotFoundForUnavailableProductDetail() throws Exception {
		when(productService.detailOnSale(10L)).thenThrow(
				new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"));

		mockMvc.perform(get("/api/products/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.message").value("商品不存在"));

		verify(productService).detailOnSale(10L);
	}

	private Product onSaleProduct() {
		Product product = new Product();
		product.setId(10L);
		product.setCategoryId(1L);
		product.setCategoryName("测试分类");
		product.setName("测试商品");
		product.setPrice(new BigDecimal("99.99"));
		product.setStock(8);
		product.setSales(0);
		product.setStatus(ProductStatus.ON_SALE);
		return product;
	}
}
