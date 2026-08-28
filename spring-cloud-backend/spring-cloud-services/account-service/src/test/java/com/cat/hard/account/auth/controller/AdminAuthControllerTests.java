package com.cat.hard.account.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.account.auth.service.AdminLoginService;
import com.cat.hard.account.auth.service.JwtLogoutService;
import com.cat.hard.account.auth.service.JwtSessionTokenService;
import com.cat.hard.account.auth.service.LoginRateLimitService;
import com.cat.hard.account.common.exception.GlobalExceptionHandler;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;
import com.cat.hard.account.user.enums.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTests {

	private MockMvc mockMvc;

	@Mock
	private AdminLoginService adminLoginService;

	@Mock
	private JwtSessionTokenService jwtSessionTokenService;

	@Mock
	private LoginRateLimitService loginRateLimitService;

	@Mock
	private JwtLogoutService jwtLogoutService;

	@InjectMocks
	private AdminAuthController adminAuthController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(adminAuthController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void shouldLoginAdminSuccessfully() throws Exception {
		User admin = new User();
		admin.setId(1L);
		admin.setUsername("admin");
		admin.setNickname("Admin User");
		admin.setRole(UserRole.ADMIN);
		admin.setStatus(UserStatus.ENABLED);

		when(loginRateLimitService.executeAdminLogin(eq("admin"), any()))
				.thenReturn(admin);
		when(jwtSessionTokenService.generateToken(1L, UserRole.ADMIN))
				.thenReturn("mocked-admin-token");

		mockMvc.perform(post("/api/admin/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "admin",
								  "password": "adminPassword123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.userId").value(1))
				.andExpect(jsonPath("$.data.username").value("admin"))
				.andExpect(jsonPath("$.data.role").value("ADMIN"))
				.andExpect(jsonPath("$.data.token").value("mocked-admin-token"));
	}

	@Test
	void shouldLogoutAdminSuccessfully() throws Exception {
		mockMvc.perform(post("/api/admin/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"));

		verify(jwtLogoutService).logout("Bearer admin-token");
	}
}
