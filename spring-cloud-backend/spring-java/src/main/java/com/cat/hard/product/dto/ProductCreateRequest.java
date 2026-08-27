package com.cat.hard.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductCreateRequest {

	@NotNull(message = "商品分类不能为空")
	@Positive(message = "商品分类ID必须大于0")
	private Long categoryId;

	@NotBlank(message = "商品名称不能为空")
	@Size(max = 128, message = "商品名称长度不能超过128个字符")
	private String name;

	@Size(max = 512, message = "商品图片URL长度不能超过512个字符")
	private String imageUrl;

	@Size(max = 16000, message = "商品描述长度不能超过16000个字符")
	private String description;

	@NotNull(message = "商品价格不能为空")
	@DecimalMin(value = "0.00", message = "商品价格不能小于0")
	@Digits(integer = 10, fraction = 2, message = "商品价格最多10位整数和2位小数")
	private BigDecimal price;

	@NotNull(message = "商品库存不能为空")
	@Min(value = 0, message = "商品库存不能小于0")
	private Integer stock;

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}
}
