package com.cat.hard.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SwaggerUrlLogger {

	private static final Logger log = LoggerFactory.getLogger(SwaggerUrlLogger.class);

	private final Environment environment;

	public SwaggerUrlLogger(Environment environment) {
		this.environment = environment;
	}

	@EventListener
	public void printSwaggerUrls(ServletWebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		String swaggerPath = environment.getProperty(
				"springdoc.swagger-ui.path",
				"/swagger-ui.html");
		String apiDocsPath = environment.getProperty(
				"springdoc.api-docs.path",
				"/v3/api-docs");
		String baseUrl = "http://localhost:" + port + contextPath;

		log.info("Swagger UI: {}", baseUrl + swaggerPath);
		log.info("OpenAPI JSON: {}", baseUrl + apiDocsPath);
	}
}
