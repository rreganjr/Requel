package com.rreganjr.jaxb;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.JAXBAnnotationGroupedByPatcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

public class JaxbUnmarshallingContextPatcherConfigurer {

    void createGroupedByPatcher(Annotation annotation, Annotatable annotatable) {
        UnmarshallingContext.getInstance().addPatcher(
				new JAXBAnnotationGroupedByPatcher(annotation, annotatable));;
    }
}
