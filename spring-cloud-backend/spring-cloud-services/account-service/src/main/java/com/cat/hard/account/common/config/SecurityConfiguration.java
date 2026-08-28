package com.cat.hard.account.common.config;

import com.cat.hard.account.auth.jwt.JwtAuthenticationFilter;
import com.cat.hard.account.auth.security.RestAccessDeniedHandler;
import com.cat.hard.account.auth.security.RestAuthenticationEntryPoint;

import jakarta.annotation.Resource;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

	private static final String[] USER_PATHS = {
			"/api/addresses",
			"/api/addresses/**"
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
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(OPEN_API_PATHS).permitAll()
						.requestMatchers("/internal/**").permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_HEALTH_PATHS).permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_AUTH_PATHS).permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers(USER_PATHS).hasRole("USER")
						.anyRequest().authenticated())
				.addFilterBefore(
						jwtAuthenticationFilter,
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
