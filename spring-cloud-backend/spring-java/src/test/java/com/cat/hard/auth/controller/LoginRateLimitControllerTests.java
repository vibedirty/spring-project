package com.cat.hard.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.auth.dto.LoginRequest;
import com.cat.hard.auth.service.AdminLoginService;
import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.auth.service.LoginRateLimitService;
import com.cat.hard.auth.service.LoginService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitControllerTests {

	private static final String USERNAME = "limited_user";
	private static final String ADMIN_USERNAME = "limited_admin";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private LoginService loginService;

	@MockitoBean
	private AdminLoginService adminLoginService;

	@MockitoBean
	private JwtSessionTokenService jwtSessionTokenService;

	@AfterEach
	void clearRateLimitKeys() {
		stringRedisTemplate.delete(LoginRateLimitService.rateLimitKey(
				"user", USERNAME));
		stringRedisTemplate.delete(LoginRateLimitService.rateLimitKey(
				"admin", ADMIN_USERNAME));
	}

	@Test
	void shouldTemporarilyLimitFrequentFailedUserLogins() throws Exception {
		when(loginService.login(any(LoginRequest.class)))
				.thenThrow(invalidCredentials());

		assertFirstFiveFailuresAndSixthLimited("/api/auth/login", USERNAME);

		verify(loginService, times(LoginRateLimitService.MAX_FAILED_ATTEMPTS))
				.login(any(LoginRequest.class));
	}

	@Test
	void shouldTemporarilyLimitFrequentFailedAdminLogins() throws Exception {
		when(adminLoginService.login(any(LoginRequest.class)))
				.thenThrow(invalidCredentials());

		assertFirstFiveFailuresAndSixthLimited(
				"/api/admin/auth/login", ADMIN_USERNAME);

		verify(adminLoginService, times(LoginRateLimitService.MAX_FAILED_ATTEMPTS))
				.login(any(LoginRequest.class));
	}

	private void assertFirstFiveFailuresAndSixthLimited(
			String path,
			String username) throws Exception {
		for (int index = 0; index < LoginRateLimitService.MAX_FAILED_ATTEMPTS; index++) {
			mockMvc.perform(post(path)
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(username)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(401))
					.andExpect(jsonPath("$.message").value("用户名或密码错误"));
		}

		mockMvc.perform(post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson(username)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(429))
				.andExpect(jsonPath("$.message")
						.value("登录失败次数过多，请2分钟后再试"));
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(
				ErrorCode.UNAUTHORIZED,
				"用户名或密码错误");
	}

	private String loginJson(String username) {
		return """
				{
				  "username": "%s",
				  "password": "wrong-password"
				}
				""".formatted(username);
	}
}
