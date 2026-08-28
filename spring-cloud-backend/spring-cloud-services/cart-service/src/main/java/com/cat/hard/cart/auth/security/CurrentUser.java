package com.cat.hard.cart.auth.security;

import com.cat.hard.cart.auth.jwt.JwtUserClaims;
import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	public Long getUserId() {
		return requireClaims().getUserId();
	}

	public String getRole() {
		return requireClaims().getRole();
	}

	public JwtUserClaims getClaims() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof JwtUserClaims claims) {
			return claims;
		}
		return null;
	}

	private JwtUserClaims requireClaims() {
		JwtUserClaims claims = getClaims();
		if (claims == null || claims.getUserId() == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户未登录或登录已过期");
		}
		return claims;
	}
}
