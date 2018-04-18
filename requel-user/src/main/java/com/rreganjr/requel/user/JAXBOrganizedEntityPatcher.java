/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user;

import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import com.rreganjr.requel.user.exception.NoSuchOrganizationException;

/**
 * This is used to swap the organization objects on CreatedEntity objects
 * assigned by the JAXB Unmarshaller with existing users from the database.
 * 
 * @author ron
 */
public class JAXBOrganizedEntityPatcher implements Patcher {

	private final OrganizedEntity organizedEntity;
	private final UserRepository userRepository;

	/**
	 * @param userRepository
	 * @param organizedEntity
	 */
	public JAXBOrganizedEntityPatcher(UserRepository userRepository, OrganizedEntity organizedEntity) {
		this.userRepository = userRepository;
		this.organizedEntity = organizedEntity;
	}

	@Override
	public void run() throws SAXException {
		try {
			if (organizedEntity.getOrganization() != null) {
				try {
					Organization existingOrganization = userRepository
							.findOrganizationByName(organizedEntity.getOrganization().getName());
					organizedEntity.setOrganization(existingOrganization);
				} catch (NoSuchOrganizationException e) {
					// new organization
					organizedEntity.setOrganization(userRepository.persist(organizedEntity
							.getOrganization()));
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}

}
