package com.cat.hard.category.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.category.service.CategoryService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.user.enums.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCategoryControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtSessionTokenService jwtTokenProvider;

	@MockitoBean
	private CategoryService categoryService;

	@Test
	void shouldAllowAdminToDeleteCategory() throws Exception {
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/categories/1/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"));

		verify(categoryService).delete(1L);
	}

	@Test
	void shouldReturnConflictForReferencedCategory() throws Exception {
		doThrow(new BusinessException(
				ErrorCode.BUSINESS_CONFLICT,
				"分类下存在商品，不能删除"))
				.when(categoryService).delete(1L);
		String token = jwtTokenProvider.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/categories/1/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(409))
				.andExpect(jsonPath("$.message")
						.value("分类下存在商品，不能删除"));

		verify(categoryService).delete(1L);
	}

	@Test
	void shouldRejectUserRoleForCategoryDelete() throws Exception {
		String token = jwtTokenProvider.generateToken(2L, UserRole.USER);

		mockMvc.perform(post("/api/admin/categories/1/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.message").value("没有访问权限"));

		verify(categoryService, never()).delete(1L);
	}
}
