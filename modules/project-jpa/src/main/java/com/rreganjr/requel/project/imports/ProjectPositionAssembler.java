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
package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.annotation.imports.PositionAssembler;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.requel.imports.annotation.PositionImportDraft;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.AddGlossaryTermPosition;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import org.springframework.util.StringUtils;

/**
 * Extends the base PositionAssembler to instantiate project-specific
 * Position subclasses (add-actor, add-glossary-term) as well as annotation
 * specializations so the discriminator column retains the correct type.
 */
public class ProjectPositionAssembler extends PositionAssembler {

	public ProjectPositionAssembler(UserRepository userRepository, User defaultCreatedBy) {
		super(userRepository, defaultCreatedBy);
	}

	@Override
	protected PositionImpl createPosition(PositionImportDraft draft, User createdBy) {
		String type = draft.getPositionType();
		if (StringUtils.hasText(type)) {
			switch (type) {
			case "addActorPosition":
				return new AddActorPosition(draft.getText(), createdBy);
			case "addGlossaryTermPosition":
				return new AddGlossaryTermPosition(draft.getText(), createdBy);
			case "changeSpellingPosition":
				return new ChangeSpellingPosition(draft.getText(), createdBy, draft.getText());
			case "addWordToDictionaryPosition":
				return new AddWordToDictionaryPosition(draft.getText(), createdBy);
			default:
				// fall through to base below
			}
		}
		return super.createPosition(draft, createdBy);
	}
}
