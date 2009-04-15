/*
 * $Id: Morphref.java,v 1.2 2009/01/03 10:24:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author ron
 */
@Entity
@Table(name = "morphref")
public class Morphref implements Serializable {
	static final long serialVersionUID = 0;

	private MorphrefId id;

	public Morphref() {
	}

	public Morphref(MorphrefId id) {
		this.id = id;
	}

	@EmbeddedId
	@AttributeOverrides( {
			@AttributeOverride(name = "morphid", column = @Column(name = "morphid", nullable = false, precision = 6, scale = 0)),
			@AttributeOverride(name = "pos", column = @Column(name = "pos", nullable = false, length = 2)),
			@AttributeOverride(name = "wordid", column = @Column(name = "wordid", nullable = false, precision = 6, scale = 0)) })
	public MorphrefId getId() {
		return this.id;
	}

	public void setId(MorphrefId id) {
		this.id = id;
	}

}
