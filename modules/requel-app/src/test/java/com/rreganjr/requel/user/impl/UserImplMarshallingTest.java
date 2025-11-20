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
package com.rreganjr.requel.user.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.DomainAdminUserRole;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.UserRolePermission;

class UserImplMarshallingTest {

	@Test
	void marshalledUserIncludesPasswordFields() throws Exception {
		OrganizationImpl organization = new OrganizationImpl("Test Org");
		UserImpl user = new UserImpl("user", "secret", "User Name", "user@example.com", "555-555-5555", organization, true);

		JAXBContext context = JAXBContext.newInstance(
				UserImpl.class,
				OrganizationImpl.class,
				UserRolePermission.class,
				SystemAdminUserRole.class,
				ProjectUserRole.class,
				DomainAdminUserRole.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		StringWriter writer = new StringWriter();
		marshaller.marshal(user, writer);

		String xml = writer.toString();
		assertThat(xml)
				.contains("<password>")
				.contains("<passwordSalt>")
				.contains("<passwordEncryptingAlgorithm>")
				.contains("<passwordEncryptingIterations>");
	}
}
