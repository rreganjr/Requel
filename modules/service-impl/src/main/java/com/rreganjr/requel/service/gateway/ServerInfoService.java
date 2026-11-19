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
package com.rreganjr.requel.service.gateway;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

import com.rreganjr.requel.service.api.dto.ServerInfoDto;

/**
 * Issue #449: which build of Requel this server is, from the files the build writes:
 * {@code META-INF/build-info.properties} (spring-boot-maven-plugin's {@code build-info} goal) and
 * {@code git.properties} (git-commit-id-maven-plugin). Spring Boot exposes them as the
 * {@link BuildProperties} and {@link GitProperties} beans, which also feed {@code /actuator/info}.
 * <p>
 * Either file can be missing: a run from an IDE that skipped the Maven goals, or a build from a
 * source archive with no {@code .git}. The fields it would have filled are then null, except the
 * version, which reads {@value #UNKNOWN}.
 */
@Component
public class ServerInfoService {

	static final String UNKNOWN = "unknown";

	private final ObjectProvider<BuildProperties> buildProperties;
	private final ObjectProvider<GitProperties> gitProperties;

	public ServerInfoService(ObjectProvider<BuildProperties> buildProperties,
			ObjectProvider<GitProperties> gitProperties) {
		this.buildProperties = buildProperties;
		this.gitProperties = gitProperties;
	}

	/** This server's version, commit and build time. */
	public ServerInfoDto current() {
		BuildProperties build = buildProperties.getIfAvailable();
		GitProperties git = gitProperties.getIfAvailable();
		String version = build != null && build.getVersion() != null ? build.getVersion() : UNKNOWN;
		return new ServerInfoDto(
				version,
				git != null ? git.getShortCommitId() : null,
				git != null ? iso(git.getCommitTime()) : null,
				build != null ? iso(build.getTime()) : null);
	}

	private static String iso(Instant instant) {
		return instant != null ? instant.toString() : null;
	}
}
