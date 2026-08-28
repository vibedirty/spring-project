package com.cat.hard.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.cat.hard.address.controller.AddressController;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LegacyAccountControllersConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					AuthController.class,
					AdminAuthController.class,
					AddressController.class);

	@Test
	void shouldKeepLegacyAccountControllersDisabledByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(AuthController.class);
			assertThat(context).doesNotHaveBean(AdminAuthController.class);
			assertThat(context).doesNotHaveBean(AddressController.class);
		});
	}
}
