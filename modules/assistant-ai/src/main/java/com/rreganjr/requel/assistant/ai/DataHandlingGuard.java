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

import java.util.Map;

/**
 * Issue #262: the egress check every remote {@link AiAnalysisClient} runs before it sends anything.
 * A request may reach a remote provider only when its {@code dataHandlingFlags} say
 * {@code externalProviderAllowed=true}; a false or <em>missing</em> flag is refused, so a caller
 * that forgot the flags fails closed rather than leaking.
 */
public final class DataHandlingGuard {

	public static final String EXTERNAL_PROVIDER_ALLOWED = "externalProviderAllowed";
	public static final String PROVIDER_LOCALITY = "providerLocality";
	public static final String PROVIDER = "provider";
	public static final String REDACTION_CATEGORIES = "redactionCategories";

	private DataHandlingGuard() {
	}

	/**
	 * @throws AiAnalysisException when {@code locality} is remote and the request does not allow it
	 */
	public static void requireAllowed(AiAnalysisRequest request, AiProviderLocality locality)
			throws AiAnalysisException {
		if (locality != AiProviderLocality.REMOTE) {
			return;
		}
		Map<String, Object> flags = request.dataHandlingFlags();
		Object allowed = flags == null ? null : flags.get(EXTERNAL_PROVIDER_ALLOWED);
		if (!Boolean.TRUE.equals(allowed)) {
			Long projectId = request.projectRef() == null ? null : request.projectRef().entityId();
			throw new AiAnalysisException("Project " + projectId + " does not allow sending its text"
					+ " to an external AI provider"
					+ (allowed == null ? " (the request carried no externalProviderAllowed flag)"
							: " (project setting egress.external is off)")
					+ "; no request was made");
		}
	}
}
