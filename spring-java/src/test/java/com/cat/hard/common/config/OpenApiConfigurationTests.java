package com.cat.hard.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;

import org.junit.jupiter.api.Test;

class OpenApiConfigurationTests {

	@Test
	void shouldProvideProjectInformation() {
		OpenAPI openAPI = new OpenApiConfiguration().hardOpenApi();

		assertThat(openAPI.getInfo().getTitle()).isEqualTo("Hard 电商系统 API");
		assertThat(openAPI.getInfo().getDescription()).isEqualTo("Hard 学习级电商系统后端接口文档");
		assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
	}
}
