package com.rreganjr.requel.utils.jaxb;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.springframework.stereotype.Component;

import com.rreganjr.requel.user.impl.User2UserImplAdapter;

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
