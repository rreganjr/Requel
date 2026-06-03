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
package com.rreganjr.requel.assistant.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "requel.ai")
public class AiProperties {

	private boolean enabled = false;
	private String provider = "noop";
	private String model = "noop";
	private String apiKey;
	private String apiKeyEnvironmentVariable = "REQUEL_AI_API_KEY";
	private String endpoint = "";
	private Duration timeout = Duration.ofSeconds(30);
	private int maxRetries = 2;
	private int maxInputTokens = 16000;
	private int maxOutputTokens = 4000;
	private List<String> projectAllowlist = new ArrayList<String>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiKeyEnvironmentVariable() {
		return apiKeyEnvironmentVariable;
	}

	public void setApiKeyEnvironmentVariable(String apiKeyEnvironmentVariable) {
		this.apiKeyEnvironmentVariable = apiKeyEnvironmentVariable;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getMaxInputTokens() {
		return maxInputTokens;
	}

	public void setMaxInputTokens(int maxInputTokens) {
		this.maxInputTokens = maxInputTokens;
	}

	public int getMaxOutputTokens() {
		return maxOutputTokens;
	}

	public void setMaxOutputTokens(int maxOutputTokens) {
		this.maxOutputTokens = maxOutputTokens;
	}

	public List<String> getProjectAllowlist() {
		return projectAllowlist;
	}

	public void setProjectAllowlist(List<String> projectAllowlist) {
		this.projectAllowlist = new ArrayList<String>(Objects.requireNonNull(projectAllowlist,
				"projectAllowlist"));
	}
}
