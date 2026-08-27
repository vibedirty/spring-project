package com.cat.hard.auth.security;

import com.cat.hard.auth.jwt.JwtUserClaims;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.user.enums.UserRole;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	public Long getUserId() {
		return getClaims().getUserId();
	}

	public UserRole getRole() {
		return getClaims().getRole();
	}

	private JwtUserClaims getClaims() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof JwtUserClaims)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return (JwtUserClaims) authentication.getPrincipal();
	}
}
