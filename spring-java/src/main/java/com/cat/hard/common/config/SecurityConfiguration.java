package com.cat.hard.common.config;

import com.cat.hard.auth.jwt.JwtAuthenticationFilter;
import com.cat.hard.auth.security.RestAccessDeniedHandler;
import com.cat.hard.auth.security.RestAuthenticationEntryPoint;

import jakarta.annotation.Resource;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

	@Resource
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Resource
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@Resource
	private RestAccessDeniedHandler accessDeniedHandler;

	private static final String[] OPEN_API_PATHS = {
			"/v3/api-docs/**",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/error"
	};

	private static final String[] PUBLIC_AUTH_PATHS = {
			"/api/auth/register",
			"/api/auth/login",
			"/api/admin/auth/login"
	};

	private static final String[] PUBLIC_HEALTH_PATHS = {
			"/actuator/health",
			"/actuator/health/**"
	};

	private static final String[] PUBLIC_GET_PATHS = {
			"/api/categories",
			"/api/categories/**",
			"/api/products/**"
	};

	private static final String[] USER_PATHS = {
			"/api/addresses",
			"/api/addresses/**",
			"/api/cart",
			"/api/cart/**",
			"/api/orders",
			"/api/orders/**"
	};

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {
		FilterRegistrationBean<JwtAuthenticationFilter> registration =
				new FilterRegistrationBean<JwtAuthenticationFilter>();
		registration.setFilter(jwtAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)//未登陆访问受保护接口处理器
						.accessDeniedHandler(accessDeniedHandler))//角色不符处理器
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(OPEN_API_PATHS).permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_HEALTH_PATHS).permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers(USER_PATHS).hasRole("USER")
						.anyRequest().authenticated())
				.addFilterBefore(
						jwtAuthenticationFilter,
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
