package com.cat.hard.account.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Supplier;

import com.cat.hard.account.auth.dto.LoginRequest;
import com.cat.hard.account.auth.dto.RegisterRequest;
import com.cat.hard.account.auth.service.JwtLogoutService;
import com.cat.hard.account.auth.service.JwtSessionTokenService;
import com.cat.hard.account.auth.service.LoginRateLimitService;
import com.cat.hard.account.auth.service.LoginService;
import com.cat.hard.account.auth.service.RegistrationService;
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
class AuthControllerTests {

	private MockMvc mockMvc;

	@Mock
	private RegistrationService registrationService;

	@Mock
	private JwtSessionTokenService jwtSessionTokenService;

	@Mock
	private LoginService loginService;

	@Mock
	private LoginRateLimitService loginRateLimitService;

	@Mock
	private JwtLogoutService jwtLogoutService;

	@InjectMocks
	private AuthController authController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void shouldRegisterUserSuccessfully() throws Exception {
		User user = new User();
		user.setId(10L);
		user.setUsername("testuser");
		user.setNickname("Test Nickname");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		when(registrationService.register(any(RegisterRequest.class))).thenReturn(user);
		when(jwtSessionTokenService.generateToken(10L, UserRole.USER)).thenReturn("mocked-jwt-token");

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "testuser",
								  "password": "password123",
								  "nickname": "Test Nickname"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.userId").value(10))
				.andExpect(jsonPath("$.data.username").value("testuser"))
				.andExpect(jsonPath("$.data.nickname").value("Test Nickname"))
				.andExpect(jsonPath("$.data.role").value("USER"))
				.andExpect(jsonPath("$.data.token").value("mocked-jwt-token"));
	}

	@Test
	void shouldLoginUserSuccessfully() throws Exception {
		User user = new User();
		user.setId(10L);
		user.setUsername("testuser");
		user.setNickname("Test Nickname");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		when(loginRateLimitService.executeUserLogin(eq("testuser"), any()))
				.thenReturn(user);
		when(jwtSessionTokenService.generateToken(10L, UserRole.USER)).thenReturn("mocked-login-token");

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "testuser",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.userId").value(10))
				.andExpect(jsonPath("$.data.username").value("testuser"))
				.andExpect(jsonPath("$.data.token").value("mocked-login-token"));
	}

	@Test
	void shouldLogoutSuccessfully() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer some-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"));

		verify(jwtLogoutService).logout("Bearer some-token");
	}
}
