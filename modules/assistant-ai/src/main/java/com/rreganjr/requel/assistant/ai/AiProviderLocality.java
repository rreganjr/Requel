/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Issue #262: whether the configured AI provider runs on this machine or somewhere else, which
 * decides whether project text leaves the installation. Remote: {@code cli} (the CLIs call their
 * vendor's API), {@code openai}, {@code anthropic}, and {@code openai-compat} unless its base-url
 * host is the local machine ({@code localhost}, {@code 127.*}, {@code ::1},
 * {@code host.docker.internal}). Local: {@code noop}. Anything unrecognised is remote, so an
 * unknown provider fails closed.
 */
public enum AiProviderLocality {
	LOCAL,
	REMOTE;

	private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "::1", "[::1]",
			"host.docker.internal");

	/** The id used in {@code dataHandlingFlags.providerLocality}. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param provider {@code requel.ai.provider}
	 * @param baseUrl {@code spring.ai.openai.base-url}; only consulted for {@code openai-compat}
	 */
	public static AiProviderLocality classify(String provider, String baseUrl) {
		String p = provider == null ? "noop" : provider.trim().toLowerCase(Locale.ROOT);
		switch (p) {
			case "noop":
				return LOCAL;
			case "openai-compat":
				return isLocalUrl(baseUrl) ? LOCAL : REMOTE;
			default:
				return REMOTE;
		}
	}

	static boolean isLocalUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			String host = URI.create(url.trim()).getHost();
			if (host == null) {
				return false;
			}
			String h = host.toLowerCase(Locale.ROOT);
			return LOCAL_HOSTS.contains(h) || h.startsWith("127.");
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
