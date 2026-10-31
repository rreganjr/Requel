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
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * The one way to open an imported project document for streaming (#443). Every import pass reads
 * through here, so they all get the same parser settings:
 * <ul>
 *   <li>DTD processing and external entities are off, and any entity the parser still tries to
 *       resolve is refused;</li>
 *   <li>a document with a DOCTYPE is rejected outright. Requel never writes one, so a DOCTYPE in a
 *       project file is either hand-edited or hostile.</li>
 * </ul>
 * A setting the parser doesn't support throws when the factory is configured, so an import never
 * runs on a parser that silently ignored one.
 */
public final class SecureXml {

    public static final String DOCTYPE_NOT_ALLOWED =
            "Project files must not contain a DOCTYPE declaration.";

    private SecureXml() {
    }

    /**
     * @return a reader over {@code in} that throws {@link ImportException} if the document has a
     *         DOCTYPE.
     * @throws XMLStreamException if the stream can't be opened as XML.
     */
    public static XMLStreamReader createStreamReader(InputStream in) throws XMLStreamException {
        XMLStreamReader reader = new DoctypeRejectingReader(newInputFactory().createXMLStreamReader(in));
        if (reader.getEventType() == XMLStreamConstants.DTD) {
            throw new ImportException(DOCTYPE_NOT_ALLOWED);
        }
        return reader;
    }

    /**
     * @return a factory with DTDs and external entities turned off. Each property is set
     *         unconditionally: a parser that rejects one throws {@link IllegalArgumentException}.
     */
    static XMLInputFactory newInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("External entities are not allowed: " + systemId);
        });
        return factory;
    }

    /** Throws as soon as the underlying reader reaches a DTD event. */
    private static final class DoctypeRejectingReader extends StreamReaderDelegate {

        DoctypeRejectingReader(XMLStreamReader reader) {
            super(reader);
        }

        @Override
        public int next() throws XMLStreamException {
            return checked(super.next());
        }

        @Override
        public int nextTag() throws XMLStreamException {
            return checked(super.nextTag());
        }

        private static int checked(int eventType) {
            if (eventType == XMLStreamConstants.DTD) {
                throw new ImportException(DOCTYPE_NOT_ALLOWED);
            }
            return eventType;
        }
    }
}
