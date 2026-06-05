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
package com.rreganjr.requel.assistant.core.context;

import java.util.List;

/**
 * Strategy interface for stripping or masking sensitive content before it
 * lands in a context pack. The default Spring bean is
 * {@link NoOpRedactionPolicy}; project- or org-aware implementations can
 * override it.
 *
 * <p>Implementations are called per text field by the context-pack builders.
 * Field paths look like {@code project.description},
 * {@code goal[42].text}, or {@code annotation.text} so the policy can apply
 * different rules per field. Implementations that drop a field should also
 * append a short note to the {@code notes} list (e.g.
 * {@code "annotation.text redacted: contains credential pattern"}); those
 * notes flow through into {@link ContextPackMetadata#redactedFields()}.</p>
 */
public interface RedactionPolicy {

	/**
	 * @param fieldPath stable path to the field (used to scope rules and to
	 *                  populate audit metadata)
	 * @param value     the field value about to be added to the pack
	 * @param notes     mutable list the policy may append redaction notes to
	 * @return the value to actually use in the pack — possibly the original
	 *         {@code value}, a masked variant, or an empty string when the
	 *         field is fully dropped
	 */
	String redact(String fieldPath, String value, List<String> notes);
}
