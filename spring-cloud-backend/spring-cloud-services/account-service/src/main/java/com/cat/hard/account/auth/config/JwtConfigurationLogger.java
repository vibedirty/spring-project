package com.cat.hard.account.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JwtConfigurationLogger implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(JwtConfigurationLogger.class);

	private final JwtProperties properties;

	public JwtConfigurationLogger(JwtProperties properties) {
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info(
				"JWT configuration loaded: expiration={}, secretConfigured={}",
				properties.expiration(),
				!properties.secret().isBlank());
	}
}
