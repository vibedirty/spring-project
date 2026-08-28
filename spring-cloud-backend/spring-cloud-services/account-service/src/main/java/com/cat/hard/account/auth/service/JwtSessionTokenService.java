package com.cat.hard.account.auth.service;

import com.cat.hard.account.auth.jwt.JwtSessionService;
import com.cat.hard.account.auth.jwt.JwtTokenProvider;
import com.cat.hard.account.auth.jwt.JwtUserClaims;
import com.cat.hard.account.user.enums.UserRole;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class JwtSessionTokenService {

	@Resource
	private JwtTokenProvider jwtTokenProvider;

	@Resource
	private JwtSessionService jwtSessionService;

	public String generateToken(Long userId, UserRole role) {
		String token = jwtTokenProvider.generateToken(userId, role);
		JwtUserClaims claims = jwtTokenProvider.parseToken(token);
		jwtSessionService.activate(token, claims);
		return token;
	}
}
