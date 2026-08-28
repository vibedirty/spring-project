package com.cat.hard.account.auth.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cat.hard.account.auth.service.JwtSessionTokenService;
import com.cat.hard.account.user.enums.UserRole;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JwtLogoutControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Resource
	private JwtSessionTokenService jwtSessionTokenService;

	@Resource
	private JwtTokenProvider jwtTokenProvider;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private String token;

	@AfterEach
	void clearSession() {
		if (token != null) {
			JwtUserClaims claims = jwtTokenProvider.parseToken(token);
			stringRedisTemplate.delete(
					JwtSessionService.sessionKey(claims.getTokenId()));
		}
	}

	@Test
	void shouldInvalidateUserTokenImmediatelyAfterLogout() throws Exception {
		token = jwtSessionTokenService.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/auth/logout")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"));

		mockMvc.perform(post("/api/auth/logout")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("Token无效或已过期"));
	}

	@Test
	void shouldInvalidateAdminTokenImmediatelyAfterLogout() throws Exception {
		token = jwtSessionTokenService.generateToken(1L, UserRole.ADMIN);

		mockMvc.perform(post("/api/admin/auth/logout")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200));

		mockMvc.perform(post("/api/admin/auth/logout")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void shouldRequireAuthenticationForLogout() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void shouldRejectSignedTokenWithoutActiveSession() throws Exception {
		token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/auth/logout")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("Token无效或已过期"));
	}
}
