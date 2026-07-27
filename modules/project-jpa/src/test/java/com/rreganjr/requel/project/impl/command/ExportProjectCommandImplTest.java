package com.rreganjr.requel.project.impl.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.StoryType;
import com.rreganjr.requel.project.impl.ActorImpl;
import com.rreganjr.requel.project.impl.GoalImpl;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.user.impl.OrganizationImpl;
import com.rreganjr.requel.user.impl.UserImpl;
import com.rreganjr.requel.utils.jaxb.JaxbAdapterConfigurer;

class ExportProjectCommandImplTest {

	@Test
	void exportsProjectXmlWithGoalsActorsAndStories() throws Exception {
		OrganizationImpl organization = new OrganizationImpl("E2E Test Org");
		setId(organization, 1L);
		UserImpl creator = new UserImpl("admin", "admin", "Admin", "admin@example.com",
				"", organization, true);
		setId(creator, 2L);
		ProjectImpl project = new ProjectImpl("Export Test", creator, organization);
		setId(project, 3L);

		GoalImpl goal = new GoalImpl(project, creator, "Roundtrip Goal", "goal text");
		setId(goal, 4L);
		ActorImpl actor = new ActorImpl(project, creator, "Roundtrip Actor", "actor text");
		setId(actor, 5L);
		project.getActors().add(actor);
		StoryImpl story = new StoryImpl(project, creator, "Roundtrip Story", "story text",
				StoryType.Success);
		setId(story, 6L);
		story.getActors().add(actor);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ExportProjectCommandImpl command = new ExportProjectCommandImpl(null, null, null,
				null, null, null, new JaxbAdapterConfigurer(), proj -> java.util.List.of());
		command.setProject(project);
		command.setOutputStream(output);

		command.execute();

		String xml = output.toString(StandardCharsets.UTF_8);
		assertThat(xml)
				.contains("<project")
				.contains("Roundtrip Goal")
				.contains("Roundtrip Actor")
				.contains("Roundtrip Story");
	}

	private static void setId(Object entity, Long id) throws Exception {
		Method setId = findSetId(entity.getClass());
		setId.setAccessible(true);
		setId.invoke(entity, id);
	}

	private static Method findSetId(Class<?> entityClass) throws NoSuchMethodException {
		Class<?> current = entityClass;
		while (current != null) {
			try {
				return current.getDeclaredMethod("setId", Long.class);
			} catch (NoSuchMethodException e) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchMethodException(entityClass.getName() + ".setId(java.lang.Long)");
	}
}
