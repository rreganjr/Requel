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

import com.rreganjr.requel.imports.ImportException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Streams {@code <tagAssignment>} elements from a project XML. String-only — the project import
 * resolves the entity references and hands them to the tagging SPI, so this module stays free of
 * any tag domain/JPA types.
 */
@Component
public class TagStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";

    private final JAXBContext jaxbContext;

    public TagStaxImporter() {
        this(createJaxbContext());
    }

    public TagStaxImporter(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    public List<TagAssignmentImportXml> readTagAssignments(InputStream inputStream) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<TagAssignmentImportXml> assignments = new ArrayList<>();

            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT
                        && NS.equals(reader.getNamespaceURI())
                        && "tagAssignment".equals(reader.getLocalName())) {
                    assignments.add(
                            unmarshaller.unmarshal(reader, TagAssignmentImportXml.class).getValue());
                    continue;
                }
                reader.next();
            }
            reader.close();
            return assignments;
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream tag assignments from XML", e);
        }
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(TagAssignmentImportXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for tag assignments", e);
        }
    }
}
