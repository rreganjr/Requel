/*
 * $Id: VerbNetSelectionRestriction.java,v 1.4 2009/02/09 10:12:29 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A restriction includes or excludes a restriction type.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnroleselres")
public class VerbNetSelectionRestriction {

	private Long id;
	private VerbNetRoleRef parent;
	private VerbNetSelectionRestrictionType type;
	private String include;

	/**
	 * @param parent
	 * @param type
	 * @param include
	 */
	public VerbNetSelectionRestriction(VerbNetRoleRef parent, VerbNetSelectionRestrictionType type,
			String include) {
		setParent(parent);
		setType(type);
		setInclude(include);
	}

	protected VerbNetSelectionRestriction() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "roleselresid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = VerbNetRoleRef.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "rolerefid", nullable = false, unique = false)
	public VerbNetRoleRef getParent() {
		return parent;
	}

	protected void setParent(VerbNetRoleRef parent) {
		this.parent = parent;
	}

	@ManyToOne(targetEntity = VerbNetSelectionRestrictionType.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "roletypeid", nullable = false, unique = false)
	public VerbNetSelectionRestrictionType getType() {
		return type;
	}

	protected void setType(VerbNetSelectionRestrictionType type) {
		this.type = type;
	}

	public String getInclude() {
		return include;
	}

	protected void setInclude(String include) {
		this.include = include;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + getInclude() + getType();
	}
}
