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
package com.rreganjr.requel.utils.jaxb.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rreganjr.requel.imports.ImportException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Issue #443: every import pass opens the document through {@link SecureXml}, which refuses a
 * DOCTYPE and never resolves an entity. The probes use a temp marker file and a local socket only.
 */
class SecureXmlTest {

    private static final String NS = "http://www.rreganjr.com/requel";
    private static final String MARKER = "REQUEL-443-MARKER";

    @TempDir
    Path tmp;

    @Test
    void aPlainDocumentReadsNormally() throws Exception {
        XMLStreamReader reader = SecureXml.createStreamReader(stream(
                "<project xmlns=\"" + NS + "\"><description>plain</description></project>"));
        StringBuilder text = new StringBuilder();
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.CHARACTERS) {
                text.append(reader.getText());
            }
        }
        assertThat(text.toString()).isEqualTo("plain");
    }

    @Test
    void aDoctypeDeclaringAnExternalEntityIsRejected() throws Exception {
        String doc = externalEntityDocument();

        assertThatThrownBy(() -> readAll(doc))
                .isInstanceOf(ImportException.class)
                .hasMessage(SecureXml.DOCTYPE_NOT_ALLOWED);
    }

    /**
     * The DOCTYPE check is the first line of defence; the factory settings are the second. Read the
     * same document through the bare factory, skipping the DTD event, and the entity still does not
     * expand.
     */
    @Test
    void theFactoryAloneNeverExpandsAnExternalEntity() throws Exception {
        String doc = externalEntityDocument();
        StringBuilder text = new StringBuilder();
        try {
            XMLStreamReader reader = SecureXml.newInputFactory().createXMLStreamReader(stream(doc));
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.CHARACTERS) {
                    text.append(reader.getText());
                }
            }
        } catch (XMLStreamException expected) {
            // an undeclared-entity parse error is the expected outcome
        }
        assertThat(text.toString()).doesNotContain(MARKER);
    }

    @Test
    void anInternalEntityExpansionIsRejected() {
        String doc = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE project [<!ENTITY a \"aaaaaaaaaa\"><!ENTITY b \"&a;&a;&a;&a;&a;\">]>"
                + "<project xmlns=\"" + NS + "\"><description>&b;</description></project>";

        assertThatThrownBy(() -> readAll(doc))
                .isInstanceOf(ImportException.class)
                .hasMessage(SecureXml.DOCTYPE_NOT_ALLOWED);
    }

    @Test
    void anExternalDtdIsRejectedWithoutAConnection() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(300);
            AtomicInteger connections = new AtomicInteger();
            Thread acceptor = new Thread(() -> {
                try (Socket ignored = server.accept()) {
                    connections.incrementAndGet();
                } catch (SocketTimeoutException timeout) {
                    // nobody connected
                } catch (Exception other) {
                    // server closed
                }
            });
            acceptor.start();
            String doc = "<?xml version=\"1.0\"?>"
                    + "<!DOCTYPE project SYSTEM \"http://127.0.0.1:" + server.getLocalPort() + "/x.dtd\">"
                    + "<project xmlns=\"" + NS + "\"><description>x</description></project>";

            assertThatThrownBy(() -> readAll(doc))
                    .isInstanceOf(ImportException.class)
                    .hasMessage(SecureXml.DOCTYPE_NOT_ALLOWED);
            acceptor.join();
            assertThat(connections).hasValue(0);
        }
    }

    /** Every importer the project import runs refuses the same document. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("importers")
    void everyImporterRejectsADoctype(String name, Consumer<InputStream> importer) throws Exception {
        String doc = externalEntityDocument();

        assertThatThrownBy(() -> importer.accept(stream(doc)))
                .isInstanceOf(ImportException.class)
                .hasMessage(SecureXml.DOCTYPE_NOT_ALLOWED);
    }

    static Stream<Arguments> importers() {
        return Stream.of(
                importer("ActorStaxImporter", in -> new ActorStaxImporter().readActors(in)),
                importer("AnnotationStaxImporter", in -> new AnnotationStaxImporter().readAnnotations(in)),
                importer("DictionaryWordStaxImporter", in -> new DictionaryWordStaxImporter().readWords(in)),
                importer("ExternalSourceStaxImporter", in -> new ExternalSourceStaxImporter().readExternalSources(in)),
                importer("GlossaryTermStaxImporter", in -> new GlossaryTermStaxImporter().readTerms(in)),
                importer("GoalStaxImporter", in -> new GoalStaxImporter().readGoals(in)),
                importer("IgnoredFindingStaxImporter", in -> new IgnoredFindingStaxImporter().readIgnoredFindings(in)),
                importer("PositionStaxImporter", in -> new PositionStaxImporter().readPositions(in)),
                importer("ReportGeneratorStaxImporter", in -> new ReportGeneratorStaxImporter().readReportGenerators(in)),
                importer("ScenarioStaxImporter", in -> new ScenarioStaxImporter().readScenarios(in)),
                importer("StakeholderStaxImporter", in -> new StakeholderStaxImporter().readStakeholders(in)),
                importer("StoryStaxImporter", in -> new StoryStaxImporter().readStories(in)),
                importer("TagStaxImporter", in -> new TagStaxImporter().readTagAssignments(in)),
                importer("UseCaseStaxImporter", in -> new UseCaseStaxImporter().readUseCases(in)),
                importer("UserStaxImporter", in -> new UserStaxImporter().readUsers(in)));
    }

    private static Arguments importer(String name, Consumer<InputStream> read) {
        return Arguments.of(name, read);
    }

    private String externalEntityDocument() throws Exception {
        Path marker = Files.writeString(tmp.resolve("marker.txt"), MARKER);
        return "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE project [<!ENTITY xxe SYSTEM \"" + marker.toUri() + "\">]>"
                + "<project xmlns=\"" + NS + "\"><description>&xxe;</description></project>";
    }

    private static void readAll(String doc) throws XMLStreamException {
        XMLStreamReader reader = SecureXml.createStreamReader(stream(doc));
        while (reader.hasNext()) {
            reader.next();
        }
    }

    private static InputStream stream(String doc) {
        return new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8));
    }
}
