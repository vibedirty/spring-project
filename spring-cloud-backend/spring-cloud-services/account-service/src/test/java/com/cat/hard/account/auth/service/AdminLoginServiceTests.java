package com.cat.hard.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cat.hard.account.auth.dto.LoginRequest;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;
import com.cat.hard.account.user.enums.UserStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTests {

	@Mock
	private LoginService loginService;

	@InjectMocks
	private AdminLoginService adminLoginService;

	@Test
	void shouldLoginAdminSuccessfully() {
		LoginRequest request = new LoginRequest();
		request.setUsername("admin");
		request.setPassword("123456");

		User admin = new User();
		admin.setId(1L);
		admin.setUsername("admin");
		admin.setRole(UserRole.ADMIN);
		admin.setStatus(UserStatus.ENABLED);

		when(loginService.authenticate(request)).thenReturn(admin);

		User result = adminLoginService.login(request);

		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
	}

	@Test
	void shouldRejectWhenUserRoleIsNotAdmin() {
		LoginRequest request = new LoginRequest();
		request.setUsername("regularUser");
		request.setPassword("123456");

		User user = new User();
		user.setId(2L);
		user.setUsername("regularUser");
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		when(loginService.authenticate(request)).thenReturn(user);

		assertThatThrownBy(() -> adminLoginService.login(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
					assertThat(ex.getMessage()).isEqualTo("仅管理员可以登录管理端");
				});
	}
}
