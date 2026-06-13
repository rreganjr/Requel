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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Requel-specific AI governance. Transport concerns (API key, endpoint/base-url, timeout, retries,
 * output-token cap, structured-output mode) are now owned by Spring AI and configured under
 * {@code spring.ai.*} (see {@code application.properties}); this class keeps only the knobs Requel
 * enforces itself:
 * <ul>
 * <li>{@code enabled} — whether the {@code RequirementsReviewAssistant} is registered;</li>
 * <li>{@code provider} — selects the active {@link AiAnalysisClient}: {@code noop} (default) vs
 * {@code openai}/{@code openai-compat} (Spring AI-backed);</li>
 * <li>{@code model} — the model id, reported in usage and bridged to
 * {@code spring.ai.openai.chat.options.model};</li>
 * <li>{@code maxInputTokens} — app-side input budget hint included in the prompt;</li>
 * <li>{@code projectAllowlist} — optional CSV of project ids permitted to use AI.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "requel.ai")
public class AiProperties {

	private boolean enabled = false;
	private String provider = "noop";
	private String model = "noop";
	private int maxInputTokens = 16000;
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

	public int getMaxInputTokens() {
		return maxInputTokens;
	}

	public void setMaxInputTokens(int maxInputTokens) {
		this.maxInputTokens = maxInputTokens;
	}

	public List<String> getProjectAllowlist() {
		return projectAllowlist;
	}

	public void setProjectAllowlist(List<String> projectAllowlist) {
		this.projectAllowlist = new ArrayList<String>(Objects.requireNonNull(projectAllowlist,
				"projectAllowlist"));
	}
}
