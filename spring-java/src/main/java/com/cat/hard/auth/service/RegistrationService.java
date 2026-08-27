package com.cat.hard.auth.service;

import java.util.Optional;

import com.cat.hard.auth.dto.RegisterRequest;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.enums.UserRole;
import com.cat.hard.user.enums.UserStatus;
import com.cat.hard.user.mapper.UserMapper;
import com.cat.hard.user.service.UserService;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

	@Resource
	private UserService userService;

	@Resource
	private UserMapper userMapper;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Transactional
	public User register(RegisterRequest request) {
		Optional<User> existingUser = userService.findByUsername(request.getUsername());
		if (existingUser.isPresent()) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "用户名已存在");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setNickname(request.getNickname());
		user.setRole(UserRole.USER);
		user.setStatus(UserStatus.ENABLED);

		try {
			userMapper.insert(user);
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "用户名已存在");
		}

		return user;
	}
}
