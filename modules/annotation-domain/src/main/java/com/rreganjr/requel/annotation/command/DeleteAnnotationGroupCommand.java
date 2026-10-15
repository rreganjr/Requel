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
package com.rreganjr.requel.annotation.command;

import com.rreganjr.platform.command.EditCommand;

/**
 * Delete every annotation whose {@code groupingObject} is the given object, links
 * first (issue #247).
 * <p>
 * A project owns its annotation group: {@code annotations.grouping_object_id} points at
 * the project through an {@code @Any} mapping with no foreign key, so an annotation left
 * behind after the project row is gone is both an orphan row and, if it is still in the
 * session when the project is deleted, a flush failure ("persistent instance references an
 * unsaved transient instance of 'null'" - the null is the {@code @Any} entity name).
 * Annotations reachable from the project's entities are deleted with those entities;
 * this command catches the rest - notes and issues an assistant filed against an entity
 * that had already been deleted, or that were left unlinked by earlier bugs - immediately
 * before the project row itself is deleted.
 * <p>
 * Internal to delete cascades; not exposed through the command API or gateway.
 *
 * @author ron
 */
public interface DeleteAnnotationGroupCommand extends EditCommand {

	/**
	 * @param groupingObject
	 *            the object (a project) whose whole annotation group is to be deleted.
	 */
	public void setGroupingObject(Object groupingObject);

	/**
	 * @return the object whose annotation group is to be deleted.
	 */
	public Object getGroupingObject();
}
