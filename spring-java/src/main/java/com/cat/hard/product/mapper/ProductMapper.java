package com.cat.hard.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.product.entity.Product;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
