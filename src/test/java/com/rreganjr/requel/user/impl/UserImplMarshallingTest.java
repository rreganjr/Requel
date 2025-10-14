package com.rreganjr.requel.user.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.DomainAdminUserRole;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.SystemAdminUserRole;
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
