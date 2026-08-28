package com.cat.hard.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.cat.hard.account.auth.dto.RegisterRequest;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;
import com.cat.hard.account.user.enums.UserStatus;
import com.cat.hard.account.user.mapper.UserMapper;
import com.cat.hard.account.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

	@Mock
	private UserService userService;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private RegistrationService registrationService;

	@Test
	void shouldRegisterUserSuccessfully() {
		RegisterRequest request = new RegisterRequest();
		request.setUsername("testuser");
		request.setPassword("123456");
		request.setNickname("Test Nickname");

		when(userService.findByUsername("testuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
		when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(100L);
			return 1;
		});

		User registered = registrationService.register(request);

		assertThat(registered.getId()).isEqualTo(100L);
		assertThat(registered.getUsername()).isEqualTo("testuser");
		assertThat(registered.getNickname()).isEqualTo("Test Nickname");
		assertThat(registered.getPassword()).isEqualTo("encodedPassword");
		assertThat(registered.getRole()).isEqualTo(UserRole.USER);
		assertThat(registered.getStatus()).isEqualTo(UserStatus.ENABLED);

		verify(userMapper).insert(any(User.class));
	}

	@Test
	void shouldRejectWhenUsernameAlreadyExists() {
		RegisterRequest request = new RegisterRequest();
		request.setUsername("existing");
		request.setPassword("123456");

		User existing = new User();
		existing.setId(1L);
		existing.setUsername("existing");
		when(userService.findByUsername("existing")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> registrationService.register(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(ex.getMessage()).isEqualTo("用户名已存在");
				});
	}

	@Test
	void shouldHandleDuplicateKeyExceptionOnInsert() {
		RegisterRequest request = new RegisterRequest();
		request.setUsername("testuser");
		request.setPassword("123456");

		when(userService.findByUsername("testuser")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
		when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));

		assertThatThrownBy(() -> registrationService.register(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(ex.getMessage()).isEqualTo("用户名已存在");
				});
	}
}
