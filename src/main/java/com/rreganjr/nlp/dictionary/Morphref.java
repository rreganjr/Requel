/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.nlp.dictionary;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
