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
package com.rreganjr.requel.tagging;

import com.rreganjr.platform.identity.User;

/**
 * SPI seam that lets the project import re-attach tags without depending on the tagging JPA module.
 *
 * <p>The project-side StAX import resolves each {@code <tagAssignment>} to its (already-imported)
 * entity and hands it here as an opaque {@code Object} plus the by-name token. The implementation
 * (in {@code tagging-jpa}) parses the token, resolves a global tag by key or find-or-creates a
 * project-scoped one, and assigns it. Because this interface names only {@code Object}, {@code String}
 * and {@link User}, {@code project-jpa} references only this contract (via {@code tagging-domain}) and
 * never {@code TagImpl} — keeping tagging a strict leaf. Mirrors the {@code TaggableTypeRegistry}
 * decoupling used by the persistence seam.</p>
 *
 * @author ron
 */
public interface TagImportHandler {

	/**
	 * Assign the tag described by {@code token} ({@code category:value[color]}) to the given
	 * taggable entity, creating or resolving the tag as needed.
	 *
	 * @param taggable the imported entity (a {@link Taggable}); its owning project provides scope
	 * @param token the by-name tag token
	 * @param createdBy the importing user, used as the tag's creator when a new tag is created
	 */
	void assignImportedTag(Object taggable, String token, User createdBy);
}
