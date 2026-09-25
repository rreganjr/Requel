/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.mcp.McpWriteService;
import com.rreganjr.requel.service.gateway.GatewayCommandController;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Regression for issue #293: a {@code @Value} placeholder must resolve through the context
 * {@code Environment}, so a test-supplied property reaches it and a non-active profile's file does
 * not.
 *
 * <p>Until #293, {@code application-config.xml} declared
 * {@code <context:property-placeholder system-properties-mode="OVERRIDE" location="classpath*:*.properties">}.
 * An explicit {@code system-properties-mode} makes Spring register the legacy
 * {@code PropertyPlaceholderConfigurer}, which reads every root {@code *.properties} file and system
 * properties but never the {@code Environment}. Under it, this IT's pin below was invisible and both
 * write-flag beans read {@code true} from {@code application.properties}, while the dev profile's
 * CORS origin reached {@code ApiSecurityConfig} in every run.
 *
 * <p>The pin is deliberately the opposite of the shipped default, so it proves the test property
 * wins. It gives this class its own Spring context.
 */
@TestPropertySource(properties = "requel.gateway.write.enabled=false")
public class PropertyPlaceholderResolutionIT extends AbstractIntegrationTestCase {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private McpWriteService mcpWriteService;

	@Autowired
	private GatewayCommandController gatewayCommandController;

	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

	@Test
	void testPropertyReachesValueInjectedBeans() {
		assertThat(mcpWriteService.isWriteEnabled())
				.as("@TestPropertySource requel.gateway.write.enabled=false must beat application.properties")
				.isFalse();
		assertThat(mcpWriteService.toolDescriptors()).isEmpty();
		assertThat(gatewayCommandController.descriptors()).isEmpty();
	}

	@Test
	void restGatewayRefusesCommandsWhenWritesDisabled() {
		ResponseEntity<?> response =
				gatewayCommandController.dispatch("EditProject", Map.of("name", "x"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void nonActiveProfileFileDoesNotReachValueInjectedBeans() {
		CorsConfiguration api = ((UrlBasedCorsConfigurationSource) corsConfigurationSource)
				.getCorsConfigurations().get("/api/**");

		assertThat(api).isNotNull();
		assertThat(api.getAllowedOrigins())
				.as("spring.cors.allowed-origins is set only by application-dev.properties; "
						+ "the dev profile is not active here")
				.isNullOrEmpty();
	}

	@Test
	void onlyEnvironmentBackedPlaceholderConfigurersAreRegistered() {
		Map<String, PlaceholderConfigurerSupport> configurers =
				applicationContext.getBeansOfType(PlaceholderConfigurerSupport.class, false, false);

		assertThat(configurers).isNotEmpty();
		assertThat(configurers.values())
				.as("a PlaceholderConfigurerSupport that is not a PropertySourcesPlaceholderConfigurer "
						+ "ignores the Environment (see #293): %s", configurers)
				.allSatisfy(c -> assertThat(c).isInstanceOf(PropertySourcesPlaceholderConfigurer.class));
	}
}
