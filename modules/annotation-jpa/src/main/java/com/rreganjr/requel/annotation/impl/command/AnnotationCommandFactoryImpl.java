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
package com.rreganjr.requel.annotation.impl.command;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.AbstractCommandFactory;
import com.rreganjr.command.CommandFactoryStrategy;
import com.rreganjr.repository.jpa.ProxyTypes;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.PositionTypes;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteArgumentCommand;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.DeleteAnnotationGroupCommand;
import com.rreganjr.requel.annotation.command.RemoveAllAnnotationsFromAnnotatableCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.PositionImpl;

/**
 * @author ron
 */
@Controller("annotationCommandFactory")
@Scope("singleton")
public class AnnotationCommandFactoryImpl extends AbstractCommandFactory implements
		AnnotationCommandFactory {

	// TODO: move the configuration to spring
	// Map of position types to resolver command types for those positions.
	private static final Map<Class<? extends Position>, Class<? extends ResolveIssueCommand>> positionToResolverCommand = new HashMap<Class<? extends Position>, Class<? extends ResolveIssueCommand>>();
	private static final Map<String, Class<? extends ResolveIssueCommand>> entityNameToResolver = new HashMap<String, Class<? extends ResolveIssueCommand>>();
	static {
		register(PositionImpl.class, ResolveIssueCommandImpl.class);
		register(ChangeSpellingPosition.class, ResolveIssueWithChangeSpellingPositionCommandImpl.class);
		register(AddWordToDictionaryPosition.class, ResolveIssueWithAddWordToDictionaryPositionCommandImpl.class);
	}

	private static void register(Class<? extends Position> positionType,
			Class<? extends ResolveIssueCommand> commandType) {
		positionToResolverCommand.put(positionType, commandType);
		entityNameToResolver.put(positionType.getName(), commandType);
	}

	/**
	 * Register a command to use to resolve a specific type of position.
	 * 
	 * @param positionType
	 * @param commandType
	 */
	public static void addPositionResolverCommand(Class<? extends Position> positionType,
			Class<? extends ResolveIssueCommand> commandType) {
		register(positionType, commandType);
	}

	/**
	 * @param creationStrategy -
	 *            the strategy to use for creating new Command instances
	 */
	@Autowired
	public AnnotationCommandFactoryImpl(CommandFactoryStrategy creationStrategy) {
		super(creationStrategy);
	}

	@Override
	public EditNoteCommand newEditNoteCommand() {
		return (EditNoteCommand) getCreationStrategy().newInstance(EditNoteCommandImpl.class);
	}

	@Override
	public EditIssueCommand newEditIssueCommand() {
		return (EditIssueCommand) getCreationStrategy().newInstance(EditIssueCommandImpl.class);
	}

	@Override
	public EditLexicalIssueCommand newEditLexicalIssueCommand() {
		return (EditLexicalIssueCommand) getCreationStrategy().newInstance(
				EditLexicalIssueCommandImpl.class);
	}

	@Override
	public ResolveIssueCommand newResolveIssueCommand(Position position) {
		// Proxy-safe type resolution — Hibernate entity name, then the persisted discriminator,
		// then the user class — lives in PositionTypes so that the resolver picked here and the
		// positionType reported to API clients can never disagree (issue #253).
		String entityName = PositionTypes.rawTypeNameOf(position);
		if (entityName != null) {
			Class<? extends ResolveIssueCommand> resolver = entityNameToResolver.get(entityName);
			if (resolver != null) {
				return (ResolveIssueCommand) getCreationStrategy().newInstance(resolver);
			}
		}

		// Fall back to the class hierarchy, for positions registered by a superclass or interface
		// rather than by their own entity name.
		Class<?> positionType = ProxyTypes.userClassOf(position);

		while (positionType != null) {
			Class<? extends ResolveIssueCommand> resolverClass = positionToResolverCommand
					.get(positionType);
			if (resolverClass == null) {
				for (Class<?> intf : positionType.getInterfaces()) {
					resolverClass = positionToResolverCommand.get(intf);
					if (resolverClass != null) {
						break;
					}
				}
			}
			if (resolverClass != null) {
				return (ResolveIssueCommand) getCreationStrategy().newInstance(resolverClass);
			}
			positionType = positionType.getSuperclass();
		}
		throw new RuntimeException("unexpected position type: " + entityName
				+ " no resolver command");
	}

	@Override
	public EditPositionCommand newEditPositionCommand() {
		return (EditPositionCommand) getCreationStrategy().newInstance(
				EditPositionCommandImpl.class);
	}

	@Override
	public EditChangeSpellingPositionCommand newEditChangeSpellingPositionCommand() {
		return (EditChangeSpellingPositionCommand) getCreationStrategy().newInstance(
				EditChangeSpellingPositionCommandImpl.class);
	}

	@Override
	public EditAddWordToDictionaryPositionCommand newEditAddWordToDictionaryPositionCommand() {
		return (EditAddWordToDictionaryPositionCommand) getCreationStrategy().newInstance(
				EditAddWordToDictionaryPositionCommandImpl.class);
	}

	@Override
	public EditArgumentCommand newEditArgumentCommand() {
		return (EditArgumentCommand) getCreationStrategy().newInstance(
				EditArgumentCommandImpl.class);
	}

	@Override
	public RemoveAnnotationFromAnnotatableCommand newRemoveAnnotationFromAnnotatableCommand() {
		return (RemoveAnnotationFromAnnotatableCommand) getCreationStrategy().newInstance(
				RemoveAnnotationFromAnnotatableCommandImpl.class);
	}

	@Override
	public RemoveAllAnnotationsFromAnnotatableCommand newRemoveAllAnnotationsFromAnnotatableCommand() {
		return (RemoveAllAnnotationsFromAnnotatableCommand) getCreationStrategy().newInstance(
				RemoveAllAnnotationsFromAnnotatableCommandImpl.class);
	}

	@Override
	public DeleteAnnotationGroupCommand newDeleteAnnotationGroupCommand() {
		return (DeleteAnnotationGroupCommand) getCreationStrategy().newInstance(
				DeleteAnnotationGroupCommandImpl.class);
	}

	@Override
	public DeleteArgumentCommand newDeleteArgumentCommand() {
		return (DeleteArgumentCommand) getCreationStrategy().newInstance(
				DeleteArgumentCommandImpl.class);
	}

	@Override
	public DeleteIssueCommand newDeleteIssueCommand() {
		return (DeleteIssueCommand) getCreationStrategy().newInstance(DeleteIssueCommandImpl.class);
	}

	@Override
	public DeleteNoteCommand newDeleteNoteCommand() {
		return (DeleteNoteCommand) getCreationStrategy().newInstance(DeleteNoteCommandImpl.class);
	}

	@Override
	public DeletePositionCommand newDeletePositionCommand() {
		return (DeletePositionCommand) getCreationStrategy().newInstance(
				DeletePositionCommandImpl.class);
	}
}
