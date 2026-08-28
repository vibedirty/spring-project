package com.cat.hard.cart.auth.security;

import java.io.IOException;

import com.cat.hard.cart.common.error.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	@Resource
	private SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {

		errorResponseWriter.writeError(
				response,
				ErrorCode.FORBIDDEN,
				ErrorCode.FORBIDDEN.getMessage());
	}
}
