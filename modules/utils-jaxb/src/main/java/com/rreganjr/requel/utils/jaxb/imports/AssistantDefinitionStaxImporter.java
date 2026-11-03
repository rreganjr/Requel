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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Issue #371: streams the {@code <assistantDefinition>} elements out of a project file.
 */
public class AssistantDefinitionStaxImporter {

    private static final String NS = "http://www.rreganjr.com/requel";

    private final JAXBContext jaxbContext;

    public AssistantDefinitionStaxImporter() {
        try {
            this.jaxbContext = JAXBContext.newInstance(AssistantDefinitionXml.class);
        } catch (JAXBException e) {
            throw new ImportException("Unable to initialize JAXB context for assistant definitions", e);
        }
    }

    public List<AssistantDefinitionXml> readAssistantDefinitions(InputStream inputStream) {
        try {
            XMLStreamReader reader = SecureXml.createStreamReader(inputStream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List<AssistantDefinitionXml> definitions = new ArrayList<>();
            while (reader.hasNext()) {
                if (reader.getEventType() == XMLStreamConstants.START_ELEMENT
                        && NS.equals(reader.getNamespaceURI())
                        && "assistantDefinition".equals(reader.getLocalName())) {
                    definitions.add(unmarshaller.unmarshal(reader, AssistantDefinitionXml.class)
                            .getValue());
                    continue;
                }
                reader.next();
            }
            reader.close();
            return definitions;
        } catch (XMLStreamException | JAXBException e) {
            throw new ImportException("Failed to stream assistant definitions from XML", e);
        }
    }
}
