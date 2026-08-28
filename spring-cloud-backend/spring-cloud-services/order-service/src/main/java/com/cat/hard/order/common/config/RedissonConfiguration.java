package com.cat.hard.order.common.config;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfiguration {

	private static final Logger log = LoggerFactory.getLogger(RedissonConfiguration.class);

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(RedissonClient.class)
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
		try {
			return Redisson.create(config);
		}
		catch (Exception e) {
			log.warn("无法连接至 Redis (redis://{}:{})，降级为 Proxy RedissonClient: {}", host, port, e.getMessage());
			return createFallbackRedissonClient();
		}
	}

	private static RedissonClient createFallbackRedissonClient() {
		RLock fallbackLock = (RLock) Proxy.newProxyInstance(
				RedissonConfiguration.class.getClassLoader(),
				new Class<?>[] { RLock.class },
				(proxy, method, args) -> {
					if ("tryLock".equals(method.getName())) {
						return true;
					}
					if ("isHeldByCurrentThread".equals(method.getName())) {
						return true;
					}
					if ("unlock".equals(method.getName()) || "lock".equals(method.getName())) {
						return null;
					}
					return null;
				});

		return (RedissonClient) Proxy.newProxyInstance(
				RedissonConfiguration.class.getClassLoader(),
				new Class<?>[] { RedissonClient.class },
				(proxy, method, args) -> {
					if ("getLock".equals(method.getName())) {
						return fallbackLock;
					}
					if ("shutdown".equals(method.getName()) || "isShutdown".equals(method.getName())) {
						return null;
					}
					return null;
				});
	}
}
