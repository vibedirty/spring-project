package com.cat.hard.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	public OpenAPI hardOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Hard 电商系统 API")
						.description("Hard 学习级电商系统后端接口文档")
						.version("v1"));
	}
}
