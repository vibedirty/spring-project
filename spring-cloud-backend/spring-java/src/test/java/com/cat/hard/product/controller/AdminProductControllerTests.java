package com.cat.hard.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.dto.ProductCreateRequest;
import com.cat.hard.product.dto.ProductPageRequest;
import com.cat.hard.product.dto.ProductUpdateRequest;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.service.ProductService;
import com.cat.hard.stock.dto.StockAdjustmentRequest;
import com.cat.hard.stock.service.StockService;
import com.cat.hard.user.enums.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminProductControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtSessionTokenService jwtTokenProvider;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private StockService stockService;

	@Test
	void shouldAllowAdminToCreateProduct() throws Exception {
		when(productService.create(any(ProductCreateRequest.class)))
				.thenReturn(createdProduct());
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.categoryId").value(1))
				.andExpect(jsonPath("$.data.categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.name").value("测试商品"))
				.andExpect(jsonPath("$.data.price").value(99.99))
				.andExpect(jsonPath("$.data.stock").value(8))
				.andExpect(jsonPath("$.data.sales").value(0))
				.andExpect(jsonPath("$.data.status").value("DRAFT"));

		verify(productService).create(any(ProductCreateRequest.class));
	}

	@Test
	void shouldRejectUserRole() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(post("/api/admin/products")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(productService, never()).create(any(ProductCreateRequest.class));
	}

	@Test
	void shouldRejectInvalidRequest() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(productService, never()).create(any(ProductCreateRequest.class));
	}

	@Test
	void shouldAllowAdminToUpdateProductBaseInfo() throws Exception {
		Product updatedProduct = createdProduct();
		updatedProduct.setName("更新商品");
		updatedProduct.setPrice(new BigDecimal("199.99"));
		when(productService.update(org.mockito.ArgumentMatchers.eq(10L),
				any(ProductUpdateRequest.class))).thenReturn(updatedProduct);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/update")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content(validUpdateRequestJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.name").value("更新商品"))
				.andExpect(jsonPath("$.data.price").value(199.99))
				.andExpect(jsonPath("$.data.stock").value(8));

		verify(productService).update(org.mockito.ArgumentMatchers.eq(10L),
				any(ProductUpdateRequest.class));
	}

	@Test
	void shouldRejectInvalidUpdateRequest() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/update")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(productService, never()).update(
				org.mockito.ArgumentMatchers.anyLong(),
				any(ProductUpdateRequest.class));
	}

	@Test
	void shouldAllowAdminToChangeProductStatus() throws Exception {
		Product onSaleProduct = createdProduct();
		onSaleProduct.setStatus(ProductStatus.ON_SALE);
		when(productService.changeStatus(10L, ProductStatus.ON_SALE))
				.thenReturn(onSaleProduct);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/change-status")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"ON_SALE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.status").value("ON_SALE"));

		verify(productService).changeStatus(10L, ProductStatus.ON_SALE);
	}

	@Test
	void shouldRejectMissingStatusInChangeRequest() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/change-status")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("目标状态不能为空"));

		verify(productService, never()).changeStatus(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(ProductStatus.class));
	}

	@Test
	void shouldRejectUnknownProductStatus() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/change-status")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"UNKNOWN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400));

		verify(productService, never()).changeStatus(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.any(ProductStatus.class));
	}

	@Test
	void shouldAllowAdminToIncreaseStock() throws Exception {
		Product product = createdProduct();
		product.setStock(13);
		when(stockService.increase(
				org.mockito.ArgumentMatchers.eq(10L),
				any(StockAdjustmentRequest.class))).thenReturn(product);
		when(productService.detail(10L)).thenReturn(product);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/stock-adjustments")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "changeQuantity": 5,
							  "reason": "管理员补充库存"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.stock").value(13));

		verify(stockService).increase(
				org.mockito.ArgumentMatchers.eq(10L),
				any(StockAdjustmentRequest.class));
		verify(productService).detail(10L);
		verify(stockService, never()).decrease(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
	}

	@Test
	void shouldAllowAdminToDecreaseStock() throws Exception {
		Product product = createdProduct();
		product.setStock(5);
		when(stockService.decrease(
				org.mockito.ArgumentMatchers.eq(10L),
				any(StockAdjustmentRequest.class))).thenReturn(product);
		when(productService.detail(10L)).thenReturn(product);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/stock-adjustments")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "changeQuantity": -3,
							  "reason": "盘点扣减库存"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.stock").value(5));

		verify(stockService).decrease(
				org.mockito.ArgumentMatchers.eq(10L),
				any(StockAdjustmentRequest.class));
		verify(productService).detail(10L);
		verify(stockService, never()).increase(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
	}

	@Test
	void shouldRejectZeroStockAdjustment() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/stock-adjustments")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "changeQuantity": 0,
							  "reason": "无效调整"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("库存变动量不能为0"));

		verify(stockService, never()).increase(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
		verify(stockService, never()).decrease(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
	}

	@Test
	void shouldRejectUserRoleForStockAdjustment() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(post("/api/admin/products/10/stock-adjustments")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "changeQuantity": 5,
							  "reason": "管理员补充库存"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(stockService, never()).increase(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
		verify(stockService, never()).decrease(
				org.mockito.ArgumentMatchers.anyLong(),
				any(StockAdjustmentRequest.class));
	}

	@Test
	void shouldAllowAdminToPageProducts() throws Exception {
		Page<Product> productPage = new Page<Product>(1, 10, 1);
		productPage.setRecords(List.of(createdProduct()));
		when(productService.page(any(ProductPageRequest.class))).thenReturn(productPage);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/products")
					.header("Authorization", "Bearer " + token)
					.param("name", "测试")
					.param("categoryId", "1")
					.param("status", "DRAFT")
					.param("page", "1")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.result[0].id").value(10))
				.andExpect(jsonPath("$.data.result[0].categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.result[0].status").value("DRAFT"))
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.data.pages").value(1));

		verify(productService).page(any(ProductPageRequest.class));
	}

	@Test
	void shouldRejectInvalidProductPageRequest() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/products")
					.header("Authorization", "Bearer " + token)
					.param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("页码不能小于1"));

		verify(productService, never()).page(any(ProductPageRequest.class));
	}

	@Test
	void shouldAllowAdminToGetDraftProductDetail() throws Exception {
		when(productService.detail(10L)).thenReturn(createdProduct());
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(get("/api/admin/products/10")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.id").value(10))
				.andExpect(jsonPath("$.data.categoryId").value(1))
				.andExpect(jsonPath("$.data.categoryName").value("测试分类"))
				.andExpect(jsonPath("$.data.name").value("测试商品"))
				.andExpect(jsonPath("$.data.description").value("商品描述"))
				.andExpect(jsonPath("$.data.price").value(99.99))
				.andExpect(jsonPath("$.data.stock").value(8))
				.andExpect(jsonPath("$.data.sales").value(0))
				.andExpect(jsonPath("$.data.status").value("DRAFT"));

		verify(productService).detail(10L);
	}

	@Test
	void shouldRejectUserRoleForProductDetail() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(get("/api/admin/products/10")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(productService, never()).detail(any());
	}

	@Test
	void shouldAllowAdminToDeleteProduct() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"));

		verify(productService).delete(10L);
	}

	@Test
	void shouldReturnNotFoundForMissingProductDelete() throws Exception {
		doThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在"))
				.when(productService).delete(10L);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/products/10/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.message").value("商品不存在"));

		verify(productService).delete(10L);
	}

	@Test
	void shouldRejectUserRoleForProductDelete() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(post("/api/admin/products/10/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(productService, never()).delete(10L);
	}

	private String validRequestJson() {
		return """
				{
				  "categoryId": 1,
				  "name": "测试商品",
				  "imageUrl": "https://example.com/product.jpg",
				  "description": "商品描述",
				  "price": 99.99,
				  "stock": 8
				}
				""";
	}

	private String validUpdateRequestJson() {
		return """
				{
				  "categoryId": 1,
				  "name": "更新商品",
				  "imageUrl": "https://example.com/updated.jpg",
				  "description": "更新描述",
				  "price": 199.99,
				  "stock": 999
				}
				""";
	}

	private Product createdProduct() {
		LocalDateTime now = LocalDateTime.of(2026, 8, 21, 10, 0);
		Product product = new Product();
		product.setId(10L);
		product.setCategoryId(1L);
		product.setCategoryName("测试分类");
		product.setName("测试商品");
		product.setImageUrl("https://example.com/product.jpg");
		product.setDescription("商品描述");
		product.setPrice(new BigDecimal("99.99"));
		product.setStock(8);
		product.setSales(0);
		product.setStatus(ProductStatus.DRAFT);
		product.setCreatedAt(now);
		product.setUpdatedAt(now);
		return product;
	}
}
