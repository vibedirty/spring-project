package com.cat.hard.common.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
interface PaginationCheckMapper {

	@Select("SELECT 1 AS number_value UNION ALL SELECT 2 AS number_value")
	IPage<Integer> selectNumbers(Page<Integer> page);
}
