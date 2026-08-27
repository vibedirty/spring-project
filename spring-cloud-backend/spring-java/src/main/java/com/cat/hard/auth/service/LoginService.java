package com.cat.hard.auth.service;

import java.util.Optional;

import com.cat.hard.auth.dto.LoginRequest;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.enums.UserRole;
import com.cat.hard.user.enums.UserStatus;
import com.cat.hard.user.service.UserService;

import jakarta.annotation.Resource;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

	@Resource
	private UserService userService;

	@Resource
	private PasswordEncoder passwordEncoder;

	public User login(LoginRequest request) {
		User user = authenticate(request);
		if (user.getRole() != UserRole.USER) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "请使用管理员登录入口");
		}
		return user;
	}

	public User authenticate(LoginRequest request) {
		Optional<User> optionalUser = userService.findByUsername(request.getUsername());
		if (!optionalUser.isPresent()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
		}

		User user = optionalUser.get();
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
		}

		if (user.getStatus() != UserStatus.ENABLED) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
		}
		return user;
	}
}
