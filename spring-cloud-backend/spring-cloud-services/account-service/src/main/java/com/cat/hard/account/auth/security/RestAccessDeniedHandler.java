package com.cat.hard.account.auth.security;

import java.io.IOException;

import com.cat.hard.account.common.error.ErrorCode;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	@Resource
	private SecurityErrorResponseWriter responseWriter;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException exception) throws IOException, ServletException {
		responseWriter.write(response, ErrorCode.FORBIDDEN, "没有访问权限");
	}
}
