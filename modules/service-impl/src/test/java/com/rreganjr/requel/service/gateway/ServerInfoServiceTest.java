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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

import com.rreganjr.requel.service.api.dto.ServerInfoDto;

/** Issue #449: {@link ServerInfoService} with and without the build's info files. */
class ServerInfoServiceTest {

	@Test
	void readsVersionCommitAndTimesFromTheBuildFiles() {
		Properties build = new Properties();
		build.setProperty("version", "2.2.0");
		build.setProperty("time", "1763506800000");
		Properties git = new Properties();
		git.setProperty("commit.id", "8f67ab953bdfc75f6df407e6a287656183186429");
		git.setProperty("commit.id.abbrev", "8f67ab9");
		// As git-commit-id-maven-plugin writes it with the dateFormat in requel-app's pom.
		git.setProperty("commit.time", "2025-11-18T17:00:00-0500");

		ServerInfoDto info = service(new BuildProperties(build), new GitProperties(git)).current();

		assertThat(info).isEqualTo(new ServerInfoDto("2.2.0", "8f67ab9",
				Instant.parse("2025-11-18T22:00:00Z").toString(),
				Instant.ofEpochMilli(1763506800000L).toString()));
	}

	@Test
	void withoutTheFilesOnlyTheVersionIsFilledIn() {
		assertThat(service(null, null).current())
				.isEqualTo(new ServerInfoDto(ServerInfoService.UNKNOWN, null, null, null));
	}

	@Test
	void aBuildWithoutGitStillReportsItsVersion() {
		Properties build = new Properties();
		build.setProperty("version", "2.2.0");
		ServerInfoDto info = service(new BuildProperties(build), null).current();
		assertThat(info.version()).isEqualTo("2.2.0");
		assertThat(info.commit()).isNull();
		assertThat(info.buildTime()).isNull();
	}

	private static ServerInfoService service(BuildProperties build, GitProperties git) {
		DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
		if (build != null) {
			beans.registerSingleton("buildProperties", build);
		}
		if (git != null) {
			beans.registerSingleton("gitProperties", git);
		}
		return new ServerInfoService(beans.getBeanProvider(BuildProperties.class),
				beans.getBeanProvider(GitProperties.class));
	}
}
