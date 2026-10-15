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
package com.rreganjr.requel.validation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.rreganjr.validator.ValidationLimits;

/**
 * Verifies the {@code tns:artifactName} length bound added to {@code doc/samples/project.xsd} by
 * issue #171.
 *
 * <p>Why the schema needs the bound at all, given the entities already have {@code @Size}: import
 * unmarshals straight into the JPA entities and never calls {@code Unmarshaller.setSchema}, so the
 * XSD is not what rejects a bad import — the entity constraints are. What the schema is, is the
 * published contract for the interchange format, and a contract that says an artifact name is an
 * unbounded {@code xs:string} is simply wrong now. This makes an over-long name diagnosable from the
 * document alone, by anyone validating a file before sending it.
 *
 * <p>Both round-trip ITs already validate *exported* XML against this schema, so those cover the
 * happy direction. What they cannot show is that the restriction bites, because the entity
 * {@code @Size} means no exportable project can carry an over-long name in the first place. Hence
 * this test, which splices one in.
 */
@DisplayName("project.xsd artifact name length bound (#171)")
class ProjectSchemaNameLengthTest {

    private static final String SAMPLE = "doc/samples/Requel.xml";
    private static final String SCHEMA = "doc/samples/project.xsd";
    private static final String PUBLISHED_SCHEMA = "website/integration/2.0/project.xsd";

    private static byte[] sampleXml;

    @BeforeAll
    static void loadSample() throws Exception {
        try (InputStream in = ProjectSchemaNameLengthTest.class.getClassLoader()
                .getResourceAsStream(SAMPLE)) {
            if (in == null) {
                throw new IllegalStateException(SAMPLE + " not found on the test classpath. Check "
                        + "the <testResource> block in modules/requel-app/pom.xml.");
            }
            sampleXml = in.readAllBytes();
        }
    }

    @Test
    @DisplayName("the sample project still validates — the new restriction breaks nothing")
    void sampleProjectStillValidates() {
        // The longest name in Requel.xml is 41 characters, so this should stay true; it is asserted
        // because both round-trip ITs validate against this schema and would fail far less legibly.
        assertDoesNotThrow(() -> validateAgainstProjectSchema(sampleXml));
    }

    @Test
    @DisplayName("a name at exactly the limit validates")
    void nameAtTheLimitValidates() {
        byte[] xml = withFirstNameOfLength(ValidationLimits.ARTIFACT_NAME_MAX);
        assertDoesNotThrow(() -> validateAgainstProjectSchema(xml),
                "a name of exactly " + ValidationLimits.ARTIFACT_NAME_MAX
                        + " characters must satisfy the schema, or the bound is off by one");
    }

    @Test
    @DisplayName("a name one character over the limit is rejected by the schema")
    void nameOverTheLimitIsRejected() {
        byte[] xml = withFirstNameOfLength(ValidationLimits.ARTIFACT_NAME_MAX + 1);

        SAXException e = assertThrows(SAXException.class, () -> validateAgainstProjectSchema(xml),
                "the schema should reject a name longer than the column can hold");

        assertTrue(e.getMessage().contains("maxLength"),
                () -> "expected a maxLength facet violation, got: " + e.getMessage());
    }

    /** Replace the first {@code <name>} element's text with {@code length} repeated characters. */
    private static byte[] withFirstNameOfLength(int length) {
        String xml = new String(sampleXml, StandardCharsets.UTF_8);
        int open = xml.indexOf("<name>");
        int close = xml.indexOf("</name>", open);
        if (open < 0 || close < 0) {
            throw new IllegalStateException("no <name> element in " + SAMPLE + " to splice");
        }
        String spliced = xml.substring(0, open + "<name>".length())
                + "a".repeat(length)
                + xml.substring(close);
        return spliced.getBytes(StandardCharsets.UTF_8);
    }

    /** Mirrors assertXmlMatchesProjectSchema in the two round-trip ITs. */
    private static void validateAgainstProjectSchema(byte[] xmlBytes) throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlBytes);
                InputStream schemaStream = ProjectSchemaNameLengthTest.class.getClassLoader()
                        .getResourceAsStream(SCHEMA)) {
            if (schemaStream == null) {
                throw new IllegalStateException(SCHEMA + " not found on the test classpath");
            }
            StreamSource schemaSource = new StreamSource(schemaStream);
            schemaSource.setSystemId("classpath:" + SCHEMA);
            Schema schema = schemaFactory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlInput));
        }
    }

    @Test
    @DisplayName("the published schema is byte-identical to the tested one")
    void publishedSchemaMatchesTheTestedSchema() throws Exception {
        // This is the guard that was missing. website/integration/1.0/project.xsd drifted from the
        // model for years while ExportProjectCommandImpl advertised it on every exported document,
        // and it ended up not merely stale but unloadable -- a non-deterministic content model, so
        // anyone who followed the schemaLocation could not build a validator at all. Nothing failed,
        // because nothing compared them.
        //
        // #171 froze 1.0 and published the current schema at website/integration/2.0. This asserts
        // the published copy and the copy the round-trip ITs validate against never diverge again.
        // If it fails, copy doc/samples/project.xsd over website/integration/2.0/project.xsd.
        byte[] tested = readClasspath(SCHEMA);
        byte[] published = readClasspath(PUBLISHED_SCHEMA);

        assertArrayEquals(tested, published,
                PUBLISHED_SCHEMA + " has drifted from " + SCHEMA + ". The published schema is what "
                        + "every export points at, so a difference here means exported documents "
                        + "advertise a schema they may not satisfy.");
    }

    private static byte[] readClasspath(String resource) throws Exception {
        try (InputStream in = ProjectSchemaNameLengthTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " not found on the test classpath. Check "
                        + "the <testResource> blocks in modules/requel-app/pom.xml.");
            }
            return in.readAllBytes();
        }
    }
}
