package com.cat.hard.auth.service;

import com.cat.hard.auth.dto.LoginRequest;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.enums.UserRole;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class AdminLoginService {

	@Resource
	private LoginService loginService;

	public User login(LoginRequest request) {
		User user = loginService.authenticate(request);
		if (user.getRole() != UserRole.ADMIN) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以登录管理端");
		}
		return user;
	}
}
