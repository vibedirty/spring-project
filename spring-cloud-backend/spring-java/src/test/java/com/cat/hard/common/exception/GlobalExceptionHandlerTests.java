package com.cat.hard.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.common.error.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new ExceptionCheckController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	void shouldReturnUnifiedResponseForInvalidParameter() throws Exception {
		mockMvc.perform(post("/exception-check/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("商品名称不能为空"))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void shouldReturnUnifiedResponseForBusinessException() throws Exception {
		mockMvc.perform(post("/exception-check/business"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.message").value("商品库存不足"))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void shouldHideUnexpectedExceptionDetails() throws Exception {
		mockMvc.perform(get("/exception-check/unexpected"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.message").value("系统内部错误"))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@RestController
	@RequestMapping("/exception-check")
	private static class ExceptionCheckController {

		@PostMapping("/validation")
		ApiResponse<String> validation(@Valid @RequestBody ProductRequest request) {
			return ApiResponse.success(request.getName());
		}

		@PostMapping("/business")
		ApiResponse<Void> business() {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "商品库存不足");
		}

		@GetMapping("/unexpected")
		ApiResponse<Void> unexpected() {
			throw new IllegalStateException("不应返回给客户端的内部信息");
		}
	}

	private static class ProductRequest {

		@NotBlank(message = "商品名称不能为空")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
