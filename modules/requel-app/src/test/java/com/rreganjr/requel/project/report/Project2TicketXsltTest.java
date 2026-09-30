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

package com.rreganjr.requel.project.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.impl.BuiltinReportGenerators;

/**
 * Issue #275: the bundled ticket generator over an export-shaped fixture, with the JDK's XSLTC as
 * GenerateReportCommandImpl uses it. The fixture lists elements out of id order, so these tests
 * also pin that the output follows ids, never export order.
 */
public class Project2TicketXsltTest {

	private static final String FIXTURE = "emit/ticket-fixture.xml";
	private static final String GOLDEN = "emit/ticket-fixture.golden.md";

	@Test
	void theTicketMatchesTheGoldenFile() throws Exception {
		assertEquals(resource(GOLDEN), render(resource(FIXTURE)));
	}

	@Test
	void theOutputDoesNotDependOnExportOrder() throws Exception {
		String fixture = resource(FIXTURE);
		// Move the second goal in front of the first, and the stale issue to the end.
		String first = between(fixture, "<goal id=\"GOL_10\">", "</goal>");
		String second = between(fixture, "<goal id=\"GOL_9\">", "</goal>");
		String reordered = fixture.replace(first, "@@FIRST@@").replace(second, first)
				.replace("@@FIRST@@", second);
		String issue = between(fixture, "<issue id=\"ANN_23\"", "</issue>");
		reordered = reordered.replace(issue, "").replace("<note id=\"ANN_26\">", issue
				+ "\n        <note id=\"ANN_26\">");
		assertTrue(!reordered.equals(fixture), "the fixture was actually reordered");
		assertEquals(render(fixture), render(reordered));
	}

	@Test
	void openIssuesGoBySeverityThenFreshBeforeStaleThenId() throws Exception {
		String ticket = render(resource(FIXTURE));
		String issues = ticket.substring(ticket.indexOf("## Open Issues"),
				ticket.indexOf("## Resources"));
		int freshHigh = issues.indexOf("Who holds the second seat?");
		int sharedStale = issues.indexOf("Shared high, stale only on the goal.");
		int staleHigh = issues.indexOf("Stale high on the replaceable goal.");
		int low = issues.indexOf("Low, fresh.");
		assertTrue(freshHigh < sharedStale && sharedStale < staleHigh && staleHigh < low, issues);
		assertTrue(!issues.contains("Resolved, not listed."), "resolved issues are not open");
		assertTrue(!issues.contains("webinar"), "advisory issues are only counted");
	}

	@Test
	void aSharedIssueIsMarkedStaleOnlyOnTheEntityItIsStaleOn() throws Exception {
		String ticket = render(resource(FIXTURE));
		assertTrue(ticket.contains("Shared high, stale only on the goal. — on Actor \"Operator\", "
				+ "Goal \"Viewers stay invisible\" (stale: the entity changed since this was raised)"),
				ticket);
	}

	@Test
	void aScenarioWithNoStepsSaysSo() throws Exception {
		assertTrue(render(resource(FIXTURE)).contains(
				"**Scenario: Archive a room**\n\n_No steps recorded._\n"));
	}

	@Test
	void aProjectWithNoSourcesSaysSo() throws Exception {
		String fixture = resource(FIXTURE);
		String sources = between(fixture, "<externalSources>", "</externalSources>");
		String ticket = render(fixture.replace(sources, ""));
		assertTrue(ticket.contains("\nSources: none recorded\n"), ticket);
		assertTrue(!ticket.contains("## Resources"), ticket);
	}

	@Test
	void aDanglingStepReferenceStopsTheTransformAndNamesIt() throws Exception {
		String dangling = resource(FIXTURE).replace("<stepRef>STP_3</stepRef>",
				"<stepRef>STP_99</stepRef>");
		List<String> messages = new ArrayList<>();
		assertThrows(TransformerException.class, () -> render(dangling, messages));
		assertTrue(messages.stream().anyMatch(m -> m.equals(
				"The report references step STP_99 of scenario SCN_7, which is not in the project.")),
				messages.toString());
	}

	@Test
	void theGeneratorDeclaresMarkdown() throws Exception {
		Transformer transformer = factory(new ArrayList<>()).newTransformer(new StreamSource(
				new StringReader(BuiltinReportGenerators.TICKET_MARKDOWN.text())));
		assertEquals("text", transformer.getOutputProperty(OutputKeys.METHOD));
		assertEquals("text/markdown", transformer.getOutputProperty(OutputKeys.MEDIA_TYPE));
	}

	private static String render(String xml) throws Exception {
		return render(xml, new ArrayList<>());
	}

	private static String render(String xml, List<String> messages) throws Exception {
		TransformerFactory factory = factory(messages);
		Transformer transformer = factory.newTransformer(new StreamSource(
				new StringReader(BuiltinReportGenerators.TICKET_MARKDOWN.text())));
		transformer.setErrorListener(factory.getErrorListener());
		transformer.setParameter("projectVersion", "abc123def456");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
		return out.toString(StandardCharsets.UTF_8);
	}

	private static TransformerFactory factory(List<String> messages) throws Exception {
		TransformerFactory factory = TransformerFactory.newDefaultInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setErrorListener(new ErrorListener() {
			public void warning(TransformerException e) {
				messages.add(e.getMessage());
			}

			public void error(TransformerException e) throws TransformerException {
				messages.add(e.getMessage());
				throw e;
			}

			public void fatalError(TransformerException e) throws TransformerException {
				messages.add(e.getMessage());
				throw e;
			}
		});
		return factory;
	}

	private static String between(String text, String start, String end) {
		int from = text.indexOf(start);
		int to = text.indexOf(end, from) + end.length();
		return text.substring(from, to);
	}

	private static String resource(String path) throws IOException {
		try (InputStream in = Project2TicketXsltTest.class.getClassLoader().getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException(path + " is not on the test classpath");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
