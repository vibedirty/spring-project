package com.cat.hard.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.cat.hard.account.auth.dto.LoginRequest;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;
import com.cat.hard.account.user.enums.UserStatus;
import com.cat.hard.account.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTests {

	@Mock
	private UserService userService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private LoginService loginService;

	@Test
	void shouldLoginUserSuccessfully() {
		LoginRequest request = new LoginRequest();
		request.setUsername("user1");
		request.setPassword("123456");

		User user = new User();
		user.setId(1L);
		user.setUsername("user1");
		user.setPassword("encoded");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		when(userService.findByUsername("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

		User result = loginService.login(request);

		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo("user1");
	}

	@Test
	void shouldRejectWhenUserNotFound() {
		LoginRequest request = new LoginRequest();
		request.setUsername("notfound");
		request.setPassword("123456");

		when(userService.findByUsername("notfound")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> loginService.login(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
					assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
				});
	}

	@Test
	void shouldRejectWhenPasswordMismatch() {
		LoginRequest request = new LoginRequest();
		request.setUsername("user1");
		request.setPassword("wrong");

		User user = new User();
		user.setId(1L);
		user.setUsername("user1");
		user.setPassword("encoded");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		when(userService.findByUsername("user1")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

		assertThatThrownBy(() -> loginService.login(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
					assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
				});
	}

	@Test
	void shouldRejectWhenUserIsDisabled() {
		LoginRequest request = new LoginRequest();
		request.setUsername("disabledUser");
		request.setPassword("123456");

		User user = new User();
		user.setId(2L);
		user.setUsername("disabledUser");
		user.setPassword("encoded");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.DISABLED);

		when(userService.findByUsername("disabledUser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

		assertThatThrownBy(() -> loginService.login(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
					assertThat(ex.getMessage()).isEqualTo("账号已被禁用");
				});
	}

	@Test
	void shouldRejectWhenAdminTriesToLoginAsUser() {
		LoginRequest request = new LoginRequest();
		request.setUsername("adminUser");
		request.setPassword("123456");

		User user = new User();
		user.setId(3L);
		user.setUsername("adminUser");
		user.setPassword("encoded");
		user.setRole(UserRole.ADMIN);
		user.setStatus(UserStatus.ENABLED);

		when(userService.findByUsername("adminUser")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);

		assertThatThrownBy(() -> loginService.login(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
					assertThat(ex.getMessage()).isEqualTo("请使用管理员登录入口");
				});
	}
}
