package com.cat.hard.common.config;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfiguration {

	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient(
			@Value("${spring.data.redis.host:127.0.0.1}") String host,
			@Value("${spring.data.redis.port:6379}") int port,
			@Value("${spring.data.redis.database:0}") int database,
			@Value("${spring.data.redis.password:}") String password,
			@Value("${spring.data.redis.connect-timeout:2s}")
			Duration connectTimeout,
			@Value("${spring.data.redis.timeout:2s}") Duration timeout) {
		Config config = new Config();
		if (!password.isBlank()) {
			config.setCredentialsResolver(address ->
					CompletableFuture.completedFuture(
							new Credentials(null, password)));
		}
		config.useSingleServer()
				.setAddress("redis://" + host + ":" + port)
				.setDatabase(database)
				.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()))
				.setTimeout(Math.toIntExact(timeout.toMillis()));
		return Redisson.create(config);
	}
}
