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
package com.rreganjr.requel.annotation.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.annotation.Annotatable;

/**
 * Lookup rules for the text-editor registry (issue #305).
 *
 * @author ron
 */
public class DefaultAnnotatableTextEditRegistryTest {

	/** Stand-ins for an entity type and a subtype of it; never instantiated. */
	private abstract static class Parent implements Annotatable {
	}

	private abstract static class Child extends Parent {
	}

	private abstract static class Unregistered implements Annotatable {
	}

	private static AnnotatableTextEditor editor() {
		return new AnnotatableTextEditor() {

			@Override
			public String currentValue(Annotatable annotatable, String propertyName) {
				return null;
			}

			@Override
			public com.rreganjr.command.Command newEditCommand(Annotatable annotatable,
					String propertyName, String newValue,
					com.rreganjr.platform.identity.User editedBy) {
				return null;
			}
		};
	}

	@Test
	public void resolvesAnExactlyRegisteredType() {
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		AnnotatableTextEditor parentEditor = editor();
		registry.registerTextEditor(Parent.class, parentEditor);

		assertSame(parentEditor, registry.resolveTextEditor(Parent.class).orElse(null));
	}

	@Test
	public void walksUpToASuperclassWhenTheExactTypeIsNotRegistered() {
		// This is what resolves a Hibernate proxy: the instance's class is a generated
		// subclass of the entity class, and only the entity class is registered.
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		AnnotatableTextEditor parentEditor = editor();
		registry.registerTextEditor(Parent.class, parentEditor);

		assertSame(parentEditor, registry.resolveTextEditor(Child.class).orElse(null));
	}

	@Test
	public void prefersTheSubclassOwnEditorOverItsSuperclassOne() {
		// ScenarioImpl extends StepImpl and needs EditScenarioCommand, not
		// EditScenarioStepCommand, so an exact match has to win over the walk.
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		AnnotatableTextEditor parentEditor = editor();
		AnnotatableTextEditor childEditor = editor();
		registry.registerTextEditor(Parent.class, parentEditor);
		registry.registerTextEditor(Child.class, childEditor);

		assertSame(childEditor, registry.resolveTextEditor(Child.class).orElse(null));
		assertSame(parentEditor, registry.resolveTextEditor(Parent.class).orElse(null));
	}

	@Test
	public void returnsEmptyForAnUnregisteredTypeAndForNull() {
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		registry.registerTextEditor(Parent.class, editor());

		assertTrue(registry.resolveTextEditor(Unregistered.class).isEmpty());
		assertTrue(registry.resolveTextEditor(null).isEmpty());
	}

	@Test
	public void rejectsASecondEditorForTheSameType() {
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		registry.registerTextEditor(Parent.class, editor());

		assertThrows(IllegalArgumentException.class,
				() -> registry.registerTextEditor(Parent.class, editor()));
	}

	@Test
	public void registeringTheSameEditorTwiceIsHarmless() {
		DefaultAnnotatableTextEditRegistry registry = new DefaultAnnotatableTextEditRegistry();
		AnnotatableTextEditor parentEditor = editor();
		registry.registerTextEditor(Parent.class, parentEditor);
		registry.registerTextEditor(Parent.class, parentEditor);

		assertEquals(1, registry.getRegisteredTextEditors().size());
	}
}
