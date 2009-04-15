/*
 * $Id: GlossaryTerm2GlossaryTermImplAdapter.java,v 1.1 2008/08/13 01:29:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import edu.harvard.fas.rregan.requel.project.GlossaryTerm;

/**
 * Adapter for JAXB to convert interface GlossaryTerm to class GlossaryTermImpl and back.
 * 
 * @author ron
 */
@XmlTransient
public class GlossaryTerm2GlossaryTermImplAdapter extends XmlAdapter<GlossaryTermImpl, GlossaryTerm> {

	@Override
	public GlossaryTermImpl marshal(GlossaryTerm glossaryTerm) throws Exception {
		return (GlossaryTermImpl) glossaryTerm;
	}

	@Override
	public GlossaryTerm unmarshal(GlossaryTermImpl glossaryTerm) throws Exception {
		return glossaryTerm;
	}

}