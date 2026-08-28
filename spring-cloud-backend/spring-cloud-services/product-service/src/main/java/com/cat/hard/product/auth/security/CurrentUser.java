package com.cat.hard.product.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	public Long getUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof Long userId) {
			return userId;
		}
		if (principal instanceof String principalStr) {
			try {
				return Long.parseLong(principalStr);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
