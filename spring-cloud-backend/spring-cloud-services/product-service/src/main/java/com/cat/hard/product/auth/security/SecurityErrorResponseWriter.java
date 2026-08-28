package com.cat.hard.product.auth.security;

import java.io.IOException;

import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public void writeError(
			HttpServletResponse response,
			ErrorCode errorCode,
			String message) throws IOException {

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
				objectMapper.writeValueAsString(ApiResponse.failure(errorCode, message)));
	}
}
