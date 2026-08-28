package com.cat.hard.account.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

@Configuration
public class JacksonConfiguration {

	@Bean
	public JsonMapperBuilderCustomizer longToStringJsonMapperCustomizer() {
		return builder -> {
			SimpleModule module = new SimpleModule("long-to-string");
			module.addSerializer(Long.class, ToStringSerializer.instance);
			module.addSerializer(Long.TYPE, ToStringSerializer.instance);
			builder.addModule(module);
		};
	}

	@Bean
	public JsonMapperBuilderCustomizer bigDecimalToStringJsonMapperCustomizer(){
		return builder -> {
			SimpleModule module = new SimpleModule("bigdecimal-to-string");
			module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
			builder.addModule(module);
		};
	}
}
