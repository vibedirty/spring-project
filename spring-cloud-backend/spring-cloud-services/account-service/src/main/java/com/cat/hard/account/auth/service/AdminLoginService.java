package com.cat.hard.account.auth.service;

import com.cat.hard.account.auth.dto.LoginRequest;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.enums.UserRole;

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
