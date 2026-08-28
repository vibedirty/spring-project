package com.cat.hard.product.common.config;

import com.cat.hard.product.auth.jwt.JwtAuthenticationFilter;
import com.cat.hard.product.auth.security.RestAccessDeniedHandler;
import com.cat.hard.product.auth.security.RestAuthenticationEntryPoint;

import jakarta.annotation.Resource;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
			"/error"
	};

	private static final String[] PUBLIC_HEALTH_PATHS = {
			"/actuator/health",
			"/actuator/health/**"
	};

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
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(OPEN_API_PATHS).permitAll()
						.requestMatchers("/internal/**").permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_HEALTH_PATHS).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.addFilterBefore(
						jwtAuthenticationFilter,
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
