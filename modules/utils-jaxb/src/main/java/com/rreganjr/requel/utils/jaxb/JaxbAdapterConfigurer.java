/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.utils.jaxb;

import com.rreganjr.requel.user.impl.User2UserImplAdapter;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;

/**
 * Centralizes registration of shared JAXB adapters so domain entities do not
 * need to reference adapter classes directly.
 */
@Component
public class JaxbAdapterConfigurer {

    public void configure(Unmarshaller unmarshaller) throws JAXBException {
        unmarshaller.setAdapter(new DateAdapter());
        unmarshaller.setAdapter(new User2UserImplAdapter());
    }

    public void configure(Marshaller marshaller) throws JAXBException {
        marshaller.setAdapter(new DateAdapter());
        marshaller.setAdapter(new User2UserImplAdapter());
    }
}
