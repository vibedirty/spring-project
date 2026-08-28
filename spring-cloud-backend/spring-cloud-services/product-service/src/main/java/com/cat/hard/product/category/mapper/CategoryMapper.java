package com.cat.hard.product.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.product.category.entity.Category;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

	@Select("SELECT COALESCE(MAX(sort), 0) FROM category WHERE deleted = 0")
	Integer selectMaxSort();
}
