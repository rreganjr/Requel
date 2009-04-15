/*
 * $Id: VerbNetRole.java,v 1.3 2009/02/11 00:47:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import edu.harvard.fas.rregan.nlp.SemanticRole;

/**
 * @author ron
 */
@Entity
@Table(name = "vnroletype")
public class VerbNetRole implements java.io.Serializable, Comparable<VerbNetRole> {
	static final long serialVersionUID = 0;

	private Long id;
	private String type;
	private SemanticRole semanticRole;

	protected VerbNetRole() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "roletypeid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column(name = "type", unique = true, nullable = false)
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "semrole", nullable = false)
	public SemanticRole getSemanticRole() {
		return semanticRole;
	}

	public void setSemanticRole(SemanticRole semanticRole) {
		this.semanticRole = semanticRole;
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}
	
	@Override
	public boolean equals(Object obj) {
		return getType().equals(getType());
	}

	@Override
	public int compareTo(VerbNetRole o) {
		return getType().compareToIgnoreCase(o.getType());
	}

}
