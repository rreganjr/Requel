/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.requel.project.impl.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import com.rreganjr.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.AddGlossaryTermPosition;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddActorPositionCommandImpl;
import com.rreganjr.requel.project.impl.command.ResolveIssueWithAddGlossaryTermPositionCommandImpl;

/**
 * Central registration of position resolver commands so project-jpa entities do
 * not depend on app-layer command implementations.
 */
@Configuration
public class PositionCommandRegistration {

	@PostConstruct
	public void registerResolvers() {
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddActorPosition.class,
				ResolveIssueWithAddActorPositionCommandImpl.class);
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddGlossaryTermPosition.class,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class);
	}
}
